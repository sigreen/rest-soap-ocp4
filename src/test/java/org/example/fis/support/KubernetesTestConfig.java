/*
 *  Copyright 2005-2023 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.example.fis.support;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.example.fis.support.KubernetesTestUtil.failNotDeployed;
import static org.example.fis.support.KubernetesTestUtil.getArrayListStringProperty;
import static org.example.fis.support.KubernetesTestUtil.getArtifactId;
import static org.example.fis.support.KubernetesTestUtil.getBooleanProperty;
import static org.example.fis.support.KubernetesTestUtil.getIntProperty;
import static org.example.fis.support.KubernetesTestUtil.getResourceFileAsStream;
import static org.example.fis.support.KubernetesTestUtil.getStringProperty;
import static org.example.fis.support.KubernetesTestUtil.isNullOrEmpty;
import static java.util.Collections.singletonMap;
import static java.util.Objects.nonNull;


public class KubernetesTestConfig {


    public static final String NAMESPACE_USE_CURRENT = "kt.namespace.use.current";
    public static final String NAMESPACE_TO_USE = "kt.namespace.use.existing";
    public static final String NAMESPACE_DESTROY_ENABLED = "kt.namespace.destroy.enabled";
    public static final String NAMESPACE_PREFIX = "kt.namespace.prefix";
    public static final String RESOUCE_FILE_PATH = "kt.resource.file.path";
    public static final String KUBERNETES_MASTER = "kubernetes.master";
    public static final String KUBERNETES_USERNAME = "kubernetes.username";
    public static final String KUBERNETES_PASSWORD = "kubernetes.password";
    public static final String KUBERNETES_TIMEOUT = "kubernetes.timeout";

    public static final int DEFAULT_KUBERNETES_TIMEOUT = 300;
    public static final String ENV_DEPENDENCIES = "kt.env.dependencies";
    public static final String DEFAULT_NAMESPACE_PREFIX = "ktest";

    public static final String TARGET_DIR_PATH = System.getProperty("basedir", ".") + "/target";
    public static final String DEFAULT_RESOUCE_FILE_PATH = TARGET_DIR_PATH + "/classes/META-INF/jkube/openshift.yml";

    public static final String TEST_PROPERTIES_FILE =  "kubernetesTest.properties";


    private final Properties systemPropertiesVars = System.getProperties();

    private boolean shouldDestroyNamespace = false;

    private boolean useExistingNamespace = true;

    private String namespace;

    private String resourceFilePath;

    private String imageStreamFilePath;

    private List<String> dependencies;

    private String kubernetesMaster;

    private String kubernetesUsername;

    private String kubernetesPassword;

    private KubernetesClient kubeClient;

    private int kubernetesTimeout;

    private String mainNamespace;

    private Map<String,String> ktestLabels = singletonMap("scope","ktest");

    private KubernetesTestConfig() {
    }

    public static KubernetesTestConfig createConfig() {
        KubernetesTestConfig config = new KubernetesTestConfig();
        config.loadConfiguration();
        return config;
    }

    public KubernetesClient getClient() {
        if (kubeClient == null) {
            if (isNullOrEmpty(kubernetesMaster)) {
                kubeClient =  new DefaultKubernetesClient();
            }else {
                Config config = new ConfigBuilder()
                        .withMasterUrl(kubernetesMaster)
                        .withUsername(kubernetesUsername)
                        .withPassword(kubernetesPassword)
                        .build();
                kubeClient = new DefaultKubernetesClient(config);
            }
        }
        return kubeClient;
    }

    private void loadConfiguration() {

        Properties prop = new Properties();

        try (InputStream input = getResourceFileAsStream(TEST_PROPERTIES_FILE)) {
            // load a properties file
            if(nonNull(input)) {
                prop.load(input);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        prop.putAll(systemPropertiesVars);

        Map<String, String> testConfig = prop.entrySet().stream().collect(
                Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> String.valueOf(e.getValue()),
                        (prev, next) -> next,
                        HashMap::new)
        );
        String artifactId = null;
        try{
            artifactId = getArtifactId();
        } catch (XPathExpressionException | ParserConfigurationException |IOException| SAXException e) {
            failNotDeployed();
        }

        namespace               = generateNamespaceName(testConfig);

        resourceFilePath        = getStringProperty(RESOUCE_FILE_PATH, testConfig, DEFAULT_RESOUCE_FILE_PATH);

        imageStreamFilePath     = TARGET_DIR_PATH + "/" + artifactId + "-is.yml" ;

        dependencies            = getArrayListStringProperty(ENV_DEPENDENCIES, testConfig);

        kubernetesMaster        = getStringProperty(KUBERNETES_MASTER, testConfig, null);

        kubernetesUsername      = getStringProperty(KUBERNETES_USERNAME, testConfig, null);

        kubernetesPassword      = getStringProperty(KUBERNETES_PASSWORD, testConfig, null);

        kubernetesTimeout       = getIntProperty(KUBERNETES_TIMEOUT,testConfig,DEFAULT_KUBERNETES_TIMEOUT);

        shouldDestroyNamespace  = getBooleanProperty(NAMESPACE_DESTROY_ENABLED, testConfig, true);

        mainNamespace           = extractMainNamespaceName(imageStreamFilePath);
    }


    private String generateNamespaceName(Map<String, String> config) {
        String sessionId = UUID.randomUUID().toString().split("-")[0];
        String namespace = getBooleanProperty(NAMESPACE_USE_CURRENT, config, false)
                ? new ConfigBuilder().build().getNamespace()
                : getStringProperty(NAMESPACE_TO_USE, config, null);
        if (isNullOrEmpty(namespace)) {
            namespace = getStringProperty(NAMESPACE_PREFIX, config, DEFAULT_NAMESPACE_PREFIX) + "-" + sessionId;
            shouldDestroyNamespace = true;
            useExistingNamespace = false;
        }
        return namespace;
    }

    private  String extractMainNamespaceName(String imageStreamFilePath){
        Pattern pattern = Pattern.compile("\\s*\"namespace\"\\s*:\\s*\"[a-z0-9]([-a-z0-9]*[a-z0-9])?\".*");
        Predicate<String> namespacePredicate = pattern.asPredicate();

        Optional<String> mainNamespace = Optional.empty();

        try (Stream<String> stream = Files.lines(Paths.get(imageStreamFilePath))) {
            mainNamespace =  stream
                    .filter(namespacePredicate)
                    .map(x->pattern.matcher(x).group(1)).findFirst();
        }catch (IOException e) {
            failNotDeployed();
        }
        return mainNamespace.orElseGet(() -> new DefaultKubernetesClient().getNamespace());
    }


    // Getter


    public String getNamespace() {
        return namespace;
    }

    public String getResourceFilePath() {
        return resourceFilePath;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public boolean isUseExistingNamespace() {
        return useExistingNamespace;
    }

    public boolean isShouldDestroyNamespace() {
        return shouldDestroyNamespace;
    }

    public int getKubernetesTimeout() {
        return kubernetesTimeout;
    }

    public Map<String, String> getKtestLabels() {
        return ktestLabels;
    }

    public String getImageStreamFilePath() {
        return imageStreamFilePath;
    }

    public String getMainNamespace() {
        return mainNamespace;
    }
}