package org.rapidoid.plugins.templates;

/*
 * #%L
 * rapidoid-templates-mustache
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.util.U;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

@Authors("Nikolche Mihajlovski")
@Since("4.1.0")
public class MustacheStringTemplate extends AbstractTemplate {

	private final MustacheFactory factory;

	private final String template;

	public MustacheStringTemplate(MustacheFactory factory, String template) {
		this.factory = factory;
		this.template = template;
	}

	public void render(OutputStream output, Object... scopes) {
		Mustache mustache = factory.compile(new StringReader(template), "(string-template)");

		try {
			mustache.execute(new PrintWriter(output), scopes).flush();
		} catch (IOException e) {
			throw U.rte("Cannot render the string template!", e);
		}
	}

}