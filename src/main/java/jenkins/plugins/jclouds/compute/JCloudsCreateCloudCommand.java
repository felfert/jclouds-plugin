/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.jclouds.compute;


import hudson.Extension;
import hudson.cli.CLICommand;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;

import org.kohsuke.args4j.Argument;

import jenkins.plugins.jclouds.internal.CredentialsHelper;

/**
 * Creates a new JCloudsCloud by reading stdin as a configuration XML file.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class JCloudsCreateCloudCommand extends CLICommand {
    @Override
    public String getShortDescription() {
        return Messages.CreateCloudCommand_shortDescription();
    }

    @Argument(metaVar = "NAME", usage = "Name of the new profile to create", required = true)
    public String name;

    @Override
    protected int run() throws Exception {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        if (null != Jenkins.get().clouds.getByName(name)) {
            throw new IllegalStateException("Cloud '" + name + "' already exists");
        }

        String xml = new String(stdin.readAllBytes(), StandardCharsets.UTF_8);
        // Not great, but cloud name is final
        xml = xml.replaceFirst("<name>.*</name>", "<name>" + name + "</name>");
        JCloudsCloud c = (JCloudsCloud) Jenkins.XSTREAM.fromXML(xml);
        validateCloudCredentials(c, xml);
        Jenkins.get().clouds.add(c);
        return 0;
    }

    private String getHashAttribute(String xml, String tag) {
        Pattern p = Pattern.compile(String.format("(?s).*?<%s sha256=\"([0-9a-fA-F]+)\">.*", tag));
        Matcher m = p.matcher(xml);
        if (m.matches()) {
            return m.group(1);
        }
        return "";
    }

    private void validateCloudCredentials(JCloudsCloud c, String xml) {
        String id = c.getCloudGlobalKeyId();
        if (null == CredentialsHelper.getCredentialsById(id)) {
            throw new IllegalStateException(String.format("cloudGlobalKeyId %s does not resolve", id));
        }
        try {
            String hash = CredentialsHelper.getCredentialsHash(id);
            String ohash = getHashAttribute(xml, "cloudGlobalKeyId");
            if (! hash.equalsIgnoreCase(ohash)) {
                throw new IllegalStateException(String.format("cloudGlobalKeyId %s resolves to a different credential", id));
            }
            id = c.getCloudCredentialsId();
            if (null == CredentialsHelper.getCredentialsById(id)) {
                throw new IllegalStateException(String.format("cloudCredentialsId %s does not resolve", id));
            }
            hash = CredentialsHelper.getCredentialsHash(id);
            ohash = getHashAttribute(xml, "cloudCredentialsId");
            if (! hash.equalsIgnoreCase(ohash)) {
                throw new IllegalStateException(String.format("cloudCredentialsId %s resolves to a different credential", id));
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not calculate hashes for credentials");
        }
    }
}
