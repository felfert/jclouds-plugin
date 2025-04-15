/*
 * Copyright 2023 Fritz Elfert
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
package jenkins.plugins.jclouds.compute;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import hudson.Extension;
import hudson.cli.CLICommand;
import jenkins.model.Jenkins;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

import jenkins.plugins.jclouds.internal.CredentialsHelper;
/**
 * Exports an existing jclouds cloud to xml on stdout
 *
 * @author Fritz Elfert
 */
@Extension
public class JCloudsGetCloudCommand extends CLICommand {

    @Argument(required = true, metaVar = "PROFILE", usage = "Name of jclouds profile to use")
        public String profile = null;

    @Option(required = false, name = "-f", aliases = "--full", usage = "Include all templates of this cloud")
    private boolean full;

    @Override
    public String getShortDescription() {
        return Messages.GetCloudCommand_shortDescription();
    }

    @Override
    protected int run() throws IOException, CmdLineException {
        Jenkins.get().checkPermission(Jenkins.READ);
        final JCloudsCloud c = CliHelper.resolveCloud(profile);
        String xml = Jenkins.XSTREAM.toXML(c);
        try {
            String hash = CredentialsHelper.getCredentialsHash(c.getCloudGlobalKeyId());
            xml = xml.replaceAll("<cloudGlobalKeyId>", String.format("<cloudGlobalKeyId sha256=\"%s\">", hash));
            hash = CredentialsHelper.getCredentialsHash(c.getCloudCredentialsId());
            xml = xml.replaceAll("<cloudCredentialsId>", String.format("<cloudCredentialsId sha256=\"%s\">", hash));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not calculate hashes for credentials");
        }
        stdout.println(full ? xml : xml.replaceAll("(?s)<templates>.*</templates>", "<templates/>"));
        return 0;
    }

}
