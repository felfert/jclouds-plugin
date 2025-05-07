/*
 * Copyright 2025 Fritz Elfert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jenkins.plugins.jclouds.cli;

import com.thoughtworks.xstream.XStreamException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import hudson.Extension;
import hudson.cli.CLICommand;

import jenkins.model.Jenkins;

import jenkins.plugins.jclouds.config.ConfigExport;
import jenkins.plugins.jclouds.internal.CryptoHelper;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import org.jenkinsci.plugins.configfiles.ConfigFiles;
import org.jenkinsci.plugins.configfiles.ConfigFileStore;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.lib.configprovider.model.Config;

/**
 * Imports an all jclouds UserData from XML supplied on stdin
 *
 * @author Fritz Elfert
 */
@Extension
public class JCloudsCreateUserdataCommand extends CLICommand {

    @Option(name = "--overwrite", usage = "Overwrite existing userdata files")
    private boolean overwrite;

    @Argument(required = false, metaVar = "CREDENTIAL", usage = "ID of credential (RSA key) to encrypt data. Default: Taken from input XML")
    private String cred = null;

    @Override
    public String getShortDescription() {
        return Messages.GetUserdataCommand_shortDescription();
    }

    @Override
    protected int run() throws IOException, CmdLineException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        String xml = new String(stdin.readAllBytes(), StandardCharsets.UTF_8);
        ConfigExport ce;
        try {
            ce = (ConfigExport)Jenkins.XSTREAM.fromXML(xml);
        } catch (XStreamException e) {
            throw new IllegalStateException("Unable to parse input: " + e.toString());
        }
        List<Config> cfgs = ce.configData;
        if (null != ce.getEncryptedConfigData()) {
            if (null == cred) {
                cred = ce.credentialsId;
            }
            CryptoHelper ch = new CryptoHelper(cred);
            xml = ch.decrypt(ce.getEncryptedConfigData());
            try {
                cfgs = (List<Config>)Jenkins.XSTREAM.fromXML(xml);
            } catch (XStreamException e) {
                throw new IllegalStateException("Unable to parse input: " + e.toString());
            }
        }

        if (!overwrite) {
            // Check if any of the configs already exists
            for (Config cfg : cfgs) {
                if (null != ConfigFiles.getByIdOrNull(Jenkins.get(), cfg.id)) {
                    throw new IllegalStateException(
                            String.format("Config data with id %s already exists", cfg.id));
                }
            }
        }

        // Save the actual data
        ConfigFileStore store = GlobalConfigFiles.get();
        for (Config cfg : cfgs) {
            store.save(cfg);
        }
        return 0;
    }
}
