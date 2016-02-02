package org.rapidoid.http;

/*
 * #%L
 * rapidoid-integration-tests
 * %%
 * Copyright (C) 2014 - 2016 Nikolche Mihajlovski and contributors
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

import org.apache.http.client.ClientProtocolException;
import org.junit.After;
import org.junit.Before;
import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.commons.Arr;
import org.rapidoid.crypto.Crypto;
import org.rapidoid.http.fast.On;
import org.rapidoid.http.fast.ReqHandler;
import org.rapidoid.io.IO;
import org.rapidoid.log.Log;
import org.rapidoid.log.LogLevel;
import org.rapidoid.scan.ClasspathUtil;
import org.rapidoid.test.TestCommons;
import org.rapidoid.u.U;
import org.rapidoid.util.D;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@Authors("Nikolche Mihajlovski")
@Since("2.0.0")
public abstract class HttpTestCommons extends TestCommons {

	private static final boolean ADJUST_RESULTS = false;

	// FIXME HEAD
	private static final List<String> HTTP_VERBS = U.list("GET", "DELETE", "OPTIONS", "TRACE", "POST", "PUT", "PATCH");

	@Before
	public void openContext() {
		Log.setLogLevel(LogLevel.INFO);

		ClasspathUtil.setRootPackage("some.nonexisting.app");

		System.out.println("--- STARTING SERVER ---");

		HTTP.STATEFUL_CLIENT.reset();

		On.getDefaultSetup().http().resetConfig();
		// On.getDefaultSetup().listen();
//		U.sleep(300);

		System.out.println("--- SERVER STARTED ---");

		notFound("/");
		notFound("/a");
		notFound("/b?dgfg");
		notFound("/c?x=123");
		notFound("/else");
		notFound("/echo");
		notFound("/upload");
	}

	@After
	public void closeContext() {
		System.out.println("--- STOPPING SERVER ---");

		// On.getDefaultSetup().shutdown();
//		U.sleep(300);

		System.out.println("--- SERVER STOPPED ---");
	}

	protected String localhost(String uri) {
		return "http://localhost:8888" + uri;
	}

	protected void defaultServerSetup() {
		On.get("/echo").plain(new ReqHandler() {
			@Override
			public Object handle(Req x) throws Exception {
				return x.verb() + ":" + x.path() + ":" + x.query();
			}
		});

		On.get("/hello").html(new ReqHandler() {
			@Override
			public Object handle(Req x) {
				return "Hello";
			}
		});

		On.post("/upload").plain(new ReqHandler() {
			@Override
			public Object handle(Req x) {
				Log.info("Uploaded files", "files", x.files().keySet());
				return U.join(":", x.cookies().get("foo"), x.cookies().get("COOKIE1"), x.posted().get("a"), x.files()
						.size(), Crypto.md5(x.files().get("f1")), Crypto.md5(x.files().get("f2")), Crypto.md5(U.or(x
						.files().get("f3"), new byte[0])));
			}
		});

		On.req(new ReqHandler() {
			@Override
			public Object handle(Req x) {
				return x.response().html(U.join(":", x.verb(), x.path(), x.query()));
			}
		});
	}

	protected String resourceMD5(String filename) throws IOException, URISyntaxException {
		return Crypto.md5(IO.loadBytes(filename));
	}

	protected String upload(String uri, Map<String, String> params, Map<String, String> files) throws IOException,
			ClientProtocolException {
		Map<String, String> headers = U.map("Cookie", "COOKIE1=a", "COOKIE", "foo=bar");
		return new String(HTTP.post(localhost(uri), headers, params, files));
	}

	protected String get(String uri) {
		return new String(HTTP.get(localhost(uri)));
	}

	protected String statefulGet(String uri) {
		return new String(HTTP.STATEFUL_CLIENT.get(localhost(uri), null).get());
	}

	protected byte[] getBytes(String uri) {
		return HTTP.get(localhost(uri));
	}

	protected void onlyGet(String uri) {
		onlyReq("GET", uri);
	}

	protected void onlyPost(String uri) {
		onlyReq("POST", uri);
	}

	protected void onlyPut(String uri) {
		onlyReq("PUT", uri);
	}

	protected void onlyDelete(String uri) {
		onlyReq("DELETE", uri);
	}

	protected void getAndPost(String uri) {
		testReq("GET", uri);
		testReq("POST", uri);
		notFoundExcept(uri, "GET", "POST");
	}

	private void onlyReq(String verb, String uri) {
		testReq(verb, uri);
		notFoundExcept(uri, verb);
	}

	protected void notFoundExcept(String uri, String... exceptVerbs) {
		for (String verb : HTTP_VERBS) {
			if (Arr.indexOf(exceptVerbs, verb) < 0) {
				notFound(verb, uri);
			}
		}
	}

	protected void notFound(String uri) {
		notFoundExcept(uri);
	}

	protected void notFound(String verb, String uri) {
		String resp = fetch(verb, uri);
		String notFound = IO.load("results/404-not-found");
		U.notNull(notFound, "404-not-found");
		checkResponse(verb, uri, resp, notFound);
	}

	private void checkResponse(String verb, String uri, String actual, String expected) {
		if (!U.eq(actual, expected)) {
			D.print(verb, uri);
		}

		eq(actual, expected);
	}

	private void testReq(String verb, String uri) {
		String resp = fetch(verb, uri);

		String filename = U.path("src", "test", "resources", reqName(verb, uri));

		if (ADJUST_RESULTS) {
			File testDir = new File(filename).getParentFile();

			if (!testDir.exists()) {
				testDir.mkdirs();
			}

			IO.save(filename, resp);
		} else {
			checkResponse(verb, uri, resp, IO.load(filename));
		}
	}

	private String fetch(String verb, String uri) {
		byte[] res = HTTP.DEFAULT_CLIENT.request(verb, localhost(uri), null, null, null, null, null, null, true).get();

		String resp = new String(res);

		resp = resp.replaceFirst("Date: .*? GMT", "Date: XXXXX GMT");
		return resp;
	}

	private String reqName(String verb, String uri) {
		return U.path("results", testName(), getTestMethodName(), verb + uri.replace("/", "_").replace("?", "-"));
	}

	protected String getTestMethodName() {
		StackTraceElement[] trace = Thread.currentThread().getStackTrace();

		String method = null;

		for (StackTraceElement trc : trace) {
			String cls = trc.getClassName();
			if (cls.equals(getClass().getName())) {
				method = trc.getMethodName();
			}
		}

		U.must(method != null, "Cannot calculate the test name!");

		return method;
	}

	protected static Map<String, Object> reqResp(Req req, Resp resp) {
		return U.map("verb", req.verb(), "uri", req.uri(), "data", req.data(), "code", resp.code());
	}

}
