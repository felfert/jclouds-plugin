/*
 * Copyright 2010-2016 Adrian Cole, Andrew Bayer, Fritz Elfert, Marat Mavlyutov, Monty Taylor, Vijay Kiran et. al.
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
package jenkins.plugins.jclouds.internal;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility for replacing shell-like variable references using a map of key/value pairs.
 *
 * @author Fritz Elfert
 */
public class ReplaceHelper {

    private final Map<String, String> rmap;

    public ReplaceHelper(@Nullable final Map<String, String> replacements) {
        rmap = replacements;
    }

    @NonNull
    public String replace(@NonNull final String content) {
        String ret = content;
        if (null == rmap) {
            return ret;
        }
        for (Map.Entry e : rmap.entrySet()) {
            final String s = Pattern.quote("${" + e.getKey() + "}");
            final String r = (String) e.getValue();
            ret = Pattern.compile(s, Pattern.DOTALL).matcher(ret).replaceAll(r);
        }
        return ret;
    }
}
