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

import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.example.fis.support.KubernetesTestDeployer.deleteNamespace;
import static org.example.fis.support.KubernetesTestDeployer.deploy;


public class KubernetesTestSetup {

    protected static Logger LOG = LoggerFactory.getLogger(KubernetesTestSetup.class);

    private KubernetesTestConfig config;

    private KubernetesClient client;

    public KubernetesTestSetup(KubernetesTestConfig config) {
        this.config = config;
        this.client = config.getClient();
    }

    public void setUp() {
        LOG.info("Doing setup...");
        deploy(client, config);
        LOG.info("setup done.");
    }

    public void tearDown() {
        LOG.info("Doing teardown...");
        if(config.isShouldDestroyNamespace()) {
            deleteNamespace(client, config);
            client.rbac()
                    .roleBindings()
                    .inNamespace(config.getMainNamespace())
                    .withLabels(config.getKtestLabels())
                    .delete();
        }else{
            LOG.info("Nothing to do!");
        }
        LOG.info("Teardown done.");
    }
}