package de.huege.oidc.authcodeflowexample;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@ViewScoped
@ManagedBean(name = "hellobean")
public class HelloBean implements Serializable {
    private String token = "";

    @PostConstruct
    public void postConstruct() throws IOException {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().getRequest();
        HttpServletResponse res = (HttpServletResponse) ctx.getExternalContext().getResponse();

        if (req.getParameterMap().containsKey("code")) {
            String[] codeArr = req.getParameterMap().get("code");
            String code = String.join(" ", codeArr);

            HttpPost post = new HttpPost("http://localhost:8090/auth/realms/oidc/protocol/openid-connect/token");

            List<NameValuePair> urlParameters = new ArrayList<>();
            urlParameters.add(new BasicNameValuePair("code", code));
            urlParameters.add(new BasicNameValuePair("redirect_uri", "http://localhost:8080/authcodeflow-example/index.xhtml"));
            urlParameters.add(new BasicNameValuePair("client_id", "jee"));
            urlParameters.add(new BasicNameValuePair("grant_type", "authorization_code"));
            urlParameters.add(new BasicNameValuePair("response_type", "code id_token"));

            post.setEntity(new UrlEncodedFormEntity(urlParameters));
            Token accessToken;
            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpClient.execute(post)){

                String result = EntityUtils.toString(response.getEntity());
                System.out.println(result);
                Gson gson = new GsonBuilder().create();
                accessToken = gson.fromJson(result, Token.class);
                setToken(result);

                try {
                    checkToken(accessToken);
                } catch (ParseException | JOSEException | BadJOSEException e) {
                    e.printStackTrace();
                }

                HttpGet get = new HttpGet("http://localhost:8090/auth/realms/oidc/protocol/openid-connect/userinfo");
                get.addHeader("Authorization", "Bearer " + accessToken.getAccessToken());
                CloseableHttpResponse getRes = httpClient.execute(get);
                String userInfo = EntityUtils.toString(getRes.getEntity());
                System.out.println(userInfo);
            }
        } else {
            res.sendRedirect("http://localhost:8090/auth/realms/oidc/protocol/openid-connect/auth?client_id=jee&redirect_url=http%3A%2F%2Flocalhost%3A8080%2Fauthcodeflow%2Findex.xhtml&response_type=code");
        }
    }

    private void checkToken(Token accessToken) throws MalformedURLException, ParseException, JOSEException, BadJOSEException {
        JWKSource<SecurityContext> keySource =
                new RemoteJWKSet<>(new URL("http://localhost:8090/auth/realms/oidc/protocol/openid-connect/certs"));
        ConfigurableJWTProcessor jwtProcessor = new DefaultJWTProcessor();
        JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
        JWSKeySelector keySelector = new JWSVerificationKeySelector(expectedJWSAlg, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);
        JWTClaimsSet claimsSet = jwtProcessor.process(accessToken.getAccessToken(), null);
        System.out.println(claimsSet.toString());
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
