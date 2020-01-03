package benjaminkomen.jwiki.core;

import benjaminkomen.jwiki.util.FL;
import okhttp3.*;

import java.io.IOException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Functions which perform {@code GET} and {@code POST} requests to the MediaWiki api and returns Response objects in a
 * suitable format.
 *
 * @author Fastily
 */
class ApiClient {
    /**
     * MediaType for {@code application/octet-stream}.
     */
    private static final MediaType OCTETSTREAM = MediaType.parse("application/octet-stream");

    /**
     * HTTP client used for all requests.
     */
    private final OkHttpClient client;

    /**
     * The Wiki object tied to this ApiClient.
     */
    private Wiki wiki;

    /**
     * Constructor, create a new ApiClient for a Wiki instance.
     *
     * @param wiki  The Wiki object this ApiClient is associated with.
     * @param proxy The proxy to use. Optional param - set null to disable.
     */
    protected ApiClient(Wiki wiki, Proxy proxy) {
        this.wiki = wiki;

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .cookieJar(new JwikiCookieJar())
                .readTimeout(2, TimeUnit.MINUTES)
                .protocols(List.of(Protocol.HTTP_1_1));

        if (proxy != null) {
            builder.proxy(proxy);
        }

        client = builder.build();
    }

    /**
     * Constructor, derives an ApiClient from a source Wiki. Useful for {@code centralauth} login/credential sharing.
     *
     * @param from The source Wiki to create the new Wiki with
     * @param to   The new Wiki to apply {@code from}'s ApiClient settings on.
     */
    protected ApiClient(Wiki from, Wiki to) {
        wiki = to;
        client = from.getApiclient().client;

        JwikiCookieJar cl = (JwikiCookieJar) client.cookieJar();

        Map<String, String> l = new HashMap<>();
        cl.cj.get(from.getWikiConfiguration().getHostname()).forEach((k, v) -> {
            if (k.contains("centralauth")) {
                l.put(k, v);
            }
        });

        cl.cj.put(wiki.getWikiConfiguration().getHostname(), l);
    }

    /**
     * Create a basic Request template which serves as the basis for any Request objects.
     *
     * @param params Any URL parameters (not URL-encoded).
     * @return A new Request.Builder with default values needed to hit MediaWiki API endpoints.
     */
    private Request.Builder startReq(Map<String, String> params) {
        HttpUrl.Builder hb = wiki.getWikiConfiguration().getBaseURL().newBuilder();
        params.forEach(hb::addQueryParameter);

        return new Request.Builder().url(hb.build()).header("User-Agent", wiki.getWikiConfiguration().getUserAgent());
    }

    /**
     * Basic {@code GET} to the MediaWiki api.
     *
     * @param params Any URL parameters (not URL-encoded).
     * @return A Response object with the result of this Request.
     * @throws IOException Network error
     */
    protected Response basicGET(Map<String, String> params) throws IOException {
        return client.newCall(startReq(params).get().build()).execute();
    }

    /**
     * Basic form-data {@code POST} to the MediaWiki api.
     *
     * @param params Any URL parameters (not URL-encoded).
     * @param form   The Key-Value form parameters to {@code POST}.
     * @return A Response object with the result of this Request.
     * @throws IOException Network error
     */
    protected Response basicPOST(Map<String, String> params, Map<String, String> form) throws IOException {
        FormBody.Builder fb = new FormBody.Builder();
        form.forEach(fb::add);

        return client.newCall(startReq(params).post(fb.build()).build()).execute();
    }

    /**
     * Performs a multi-part file {@code POST}.
     *
     * @param params Any URL parameters (not URL-encoded).
     * @param form   The Key-Value form parameters to {@code POST}.
     * @param fn     The system name of the file to {@code POST}
     * @param chunk  The raw byte data associated with this file which will be sent in this {@code POST}.
     * @return A Response with the results of this {@code POST}.
     * @throws IOException Network error
     */
    protected Response multiPartFilePOST(Map<String, String> params, Map<String, String> form, String fn, byte[] chunk) throws IOException {
        MultipartBody.Builder mpb = new MultipartBody.Builder().setType(MultipartBody.FORM);
        form.forEach(mpb::addFormDataPart);

        mpb.addFormDataPart("chunk", fn, RequestBody.create(chunk, OCTETSTREAM));

        Request r = startReq(params).post(mpb.build()).build();
        return client.newCall(r).execute();
    }

    /**
     * Basic CookieJar policy for use with jwiki.
     *
     * @author Fastily
     */
    private static class JwikiCookieJar implements CookieJar {

        /**
         * Internal Map tracking cookies. Legend - [ domain : [ key : value ] ].
         */
        private Map<String, Map<String, String>> cj = new HashMap<>();

        private JwikiCookieJar() {
            // no-args constructor
        }

        /**
         * Called when receiving a Response from the Api.
         */
        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            String host = url.host();
            if (!cj.containsKey(host))
                cj.put(host, new HashMap<>());

            Map<String, String> m = cj.get(host);
            for (Cookie c : cookies)
                m.put(c.name(), c.value());
        }

        /**
         * Called when creating a new Request to the Api.
         */
        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            String host = url.host();
            return !cj.containsKey(host) ? new ArrayList<>()
                    : FL.toArrayList(cj.get(host).entrySet().stream()
                    .map(e -> new Cookie.Builder().name(e.getKey()).value(e.getValue()).domain(host).build()));
        }
    }
}