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
import java.util.List;
import java.util.Map;

import hudson.Extension;
import hudson.cli.CLICommand;
import jenkins.model.Jenkins;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;

import jenkins.plugins.jclouds.config.ConfigHelper;
import jenkins.plugins.jclouds.internal.CredentialsHelper;

/**
 * Exports an existing template to xml on stdout
 *
 * @author Fritz Elfert
 */
@Extension
public class JCloudsGetTemplateCommand extends CLICommand {

    @Argument(required = false, metaVar = "PROFILE", index = 1, usage = "Name of jclouds profile to use")
        public String profile = null;

    @Argument(required = true, metaVar = "TEMPLATE", index = 0, usage = "Name of template to export")
        public String tmpl;

    @Override
    public String getShortDescription() {
        return Messages.GetTemplateCommand_shortDescription();
    }

    @Override
    protected int run() throws IOException, CmdLineException {
        Jenkins.get().checkPermission(Jenkins.READ);
        final JCloudsCloud c = CliHelper.resolveCloud(profile);
        final JCloudsSlaveTemplate tpl = CliHelper.resolveTemplate(c, tmpl);
        stdout.println(getXmlWithHashes(tpl));
        return 0;
    }

    static protected String getXmlWithHashes(JCloudsSlaveTemplate tpl) {
        String xml = Jenkins.XSTREAM.toXML(tpl);
        try {
            String hash;
            String aid = tpl.getAdminCredentialsId();
            if (null != aid && aid.length() > 0) {
                hash = CredentialsHelper.getCredentialsHash(aid);
                xml = xml.replaceFirst("<adminCredentialsId>", String.format("<adminCredentialsId sha256=\"%s\">", hash));
            }
            hash = CredentialsHelper.getCredentialsHash(tpl.getCredentialsId());
            xml = xml.replaceFirst("<credentialsId>", String.format("<credentialsId sha256=\"%s\">", hash));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not calculate hashes for credentials");
        }
        try {
            xml = getUserDataHashes(tpl, xml);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not calculate hashes for userdata");
        }
        return xml;
    }

    static private String getUserDataHashes(JCloudsSlaveTemplate tpl, String xml) throws NoSuchAlgorithmException {
        List<String> ids = tpl.getUserDataIds();
        Map<String, String> m = ConfigHelper.getUserDataHashes(ids);
        for (String id : ids) {
            String hash = m.get(id);
            xml = xml.replaceFirst(String.format("<fileId>(%s)", id),String.format("<fileId sha256=\"%s\">$1", hash));
        }
        return xml;
    }
}
