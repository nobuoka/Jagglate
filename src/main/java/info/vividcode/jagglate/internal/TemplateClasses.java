/*
Copyright 2014 NOBUOKA Yu

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package info.vividcode.jagglate.internal;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TemplateClasses {

    public static String underscore(String str) {
        StringBuilder sb = new StringBuilder();
        Pattern topUnderScore = Pattern.compile("\\A_+");
        Matcher m = topUnderScore.matcher(str);
        if (m.find()) {
            sb.append(str.substring(0, m.end()));
            str = str.substring(m.end());
        }

        Pattern capitalLetterPattern = Pattern.compile("[A-Z]");
        m = capitalLetterPattern.matcher(str);
        int pos = 0;
        while (m.find()) {
            sb.append(str.substring(pos, m.start()));
            if (m.start() != 0) sb.append('_');
            sb.append(m.group().toLowerCase(Locale.US));
            pos = m.end();
        }
        sb.append(str.substring(pos));
        return sb.toString();
    }

    public static String camelize(String str) {
        StringBuilder sb = new StringBuilder();
        Pattern topUnderScore = Pattern.compile("\\A_+");
        Matcher m = topUnderScore.matcher(str);
        if (m.find()) {
            sb.append(str.substring(0, m.end()));
            str = str.substring(m.end());
        }

        Pattern firstLetterPattern = Pattern.compile("(?:\\A|_)([a-z])");
        m = firstLetterPattern.matcher(str);
        int pos = 0;
        while (m.find()) {
            sb.append(str.substring(pos, m.start()));
            sb.append(m.group(1).toUpperCase(Locale.US));
            pos = m.end();
        }
        sb.append(str.substring(pos));
        return sb.toString();
    }

}
