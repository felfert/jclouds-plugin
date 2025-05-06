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

import hudson.Extension;
import hudson.cli.CLICommand;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import jenkins.model.Jenkins;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import jenkins.plugins.jclouds.compute.JCloudsCloud;
import jenkins.plugins.jclouds.compute.JCloudsSlaveTemplate;
import jenkins.plugins.jclouds.internal.CredentialsHelper;

/**
 * Creates a new JCloudsCloud by reading stdin as a configuration XML file.
 *
 * @author Fritz Elfert
*/
@Extension
public class JCloudsCreateCloudCommand extends CLICommand {
    @Override
    public String getShortDescription() {
        return Messages.CreateCloudCommand_shortDescription();
    }

    @Argument(metaVar = "NAME", usage = "Name of the new profile to create", required = true)
    public String name;

    @Option(required = false, name = "-v", aliases = "--verbose", usage = "Be verbose when validating references to credentials and config files")
    private boolean verbose;

    @Override
    protected int run() throws Exception {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        if (null != Jenkins.get().clouds.getByName(name)) {
            throw new IllegalStateException("Cloud '" + name + "' already exists");
        }

        String xml = new String(stdin.readAllBytes(), StandardCharsets.UTF_8);
        // Not great, but cloud name is final
        xml = xml.replaceFirst("<name>.*</name>", "<name>" + name + "</name>");
        JCloudsCloud c = null;
        try {
          c = (JCloudsCloud) Jenkins.XSTREAM.fromXML(xml);
        } catch (XStreamException e) {
            throw new IllegalStateException("Unable to parse input: " + e.toString());
        }
        PrintStream devnull = CliHelper.getDevNull();
        validateCloudCredentials(c, xml, verbose ? stdout : devnull);
        for (JCloudsSlaveTemplate tpl : c.getTemplates()) {
            CliHelper.validateTemplate(tpl, xml, verbose ? stdout : devnull);
            tpl.setCloud(c);
        }
        Jenkins.get().clouds.add(c);
        return 0;
    }

    private void validateCloudCredentials(JCloudsCloud c, String xml, PrintStream verbose) {
        String id = c.getCloudGlobalKeyId();
        if (null == CredentialsHelper.getCredentialsById(id)) {
            throw new IllegalStateException(String.format("cloudGlobalKeyId %s does not resolve", id));
        }
        try {
            String hash = CredentialsHelper.getCredentialsHash(id);
            String ohash = CliHelper.getHashAttribute(xml, "cloudGlobalKeyId");
            if (! hash.equalsIgnoreCase(ohash)) {
                throw new IllegalStateException(String.format("cloudGlobalKeyId %s resolves to a different credential", id));
            }
            verbose.println(String.format("Validated cloudGlobalKeyId %s of cloud %s", id, c.name));
            id = c.getCloudCredentialsId();
            if (null == CredentialsHelper.getCredentialsById(id)) {
                throw new IllegalStateException(String.format("cloudCredentialsId %s does not resolve", id));
            }
            hash = CredentialsHelper.getCredentialsHash(id);
            ohash = CliHelper.getHashAttribute(xml, "cloudCredentialsId");
            if (! hash.equalsIgnoreCase(ohash)) {
                throw new IllegalStateException(String.format("cloudCredentialsId %s resolves to a different credential", id));
            }
            verbose.println(String.format("Validated cloudCredentialsId %s of cloud %s", id, c.name));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not calculate hashes for credentials");
        }
    }
}
