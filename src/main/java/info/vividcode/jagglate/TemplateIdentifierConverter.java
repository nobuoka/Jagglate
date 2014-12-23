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

package info.vividcode.jagglate;

class TemplateIdentifierConverter {

    public final String classNamePrefix = "templates.";

    public String convertPathToClassName(String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(classNamePrefix);

        String[] pp = path.split("/");
        String fileName = pp[pp.length - 1];
        for (int i = 0; i < pp.length - 1; i++) {
            sb.append(pp[i]).append('.');
        }
        String[] ff = fileName.split("\\.");
        boolean isFirst = true;
        for (String f : ff) {
            if (!isFirst) sb.append('$');
            sb.append(TemplateClasses.camelize(f));
            isFirst = false;
        }
        return sb.toString();
    }

    public String convertClassNameToPath(String className) {
        String prefix = className.substring(0, classNamePrefix.length());
        if (!prefix.equals(classNamePrefix)) {
            throw new RuntimeException("invalid className");
        }
        className = className.substring(classNamePrefix.length());

        StringBuilder sb = new StringBuilder();
        String[] cc = className.split("\\.");
        String localClassName = cc[cc.length - 1];
        for (int i = 0; i < cc.length - 1; i++) {
            sb.append(cc[i]).append('/');
        }
        String[] ff = localClassName.split("\\$");
        boolean isFirst = true;
        for (String f : ff) {
            if (!isFirst) sb.append('.');
            sb.append(TemplateClasses.underscore(f));
            isFirst = false;
        }
        return sb.toString();
    }

}
