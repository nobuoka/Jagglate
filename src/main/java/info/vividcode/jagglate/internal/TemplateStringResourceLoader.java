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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class TemplateStringResourceLoader implements TemplateStringLoader {

    private final ClassLoader mResourceLoader;

    private final Charset mFileCharset;

    private final String mPathPrefix;

    public TemplateStringResourceLoader(Charset fileCharset, String pathPrefix) {
        this(fileCharset, TemplateStringResourceLoader.class.getClassLoader(), pathPrefix);
    }

    public TemplateStringResourceLoader(Charset fileCharset, ClassLoader cl, String pathPrefix) {
        mFileCharset = fileCharset;
        mResourceLoader = cl;
        mPathPrefix = pathPrefix;
    }

    @Override
    public String load(String path) throws IOException {
        ClassLoader l = mResourceLoader;
        try (
                InputStream is = l.getResourceAsStream(mPathPrefix + path);
                BufferedInputStream bis = new BufferedInputStream(is)) {
            byte[] bb = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int count;
            while ((count = bis.read(bb)) != -1) {
                out.write(bb, 0, count);
            }
            return new String(out.toByteArray(), mFileCharset);
        }
    }

}
