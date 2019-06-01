package com.nicheware.maven;

/*
 * Copyright 2010-2011 the original author or authors.
 *
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
 *
 * LeinGitlabWagon:
 *
 * Wrapper around HttpWagon that converts authentication values for username and password
 * to a http header and value.
 *
 * Needed to be able to authenticate with GitLab private maven repositories which require HTTP headers like:
 *
 * Private-Token:  <user-private-token>
 *
 * For any repository URL of the for gitlab://<domain>/ it will:
 *
 * - If present use the authentication user name as HTTP header name
 * - If present use the authentication password as the above HTTP headers value
 * - Remove the username and password so they are not used for HTTP authentication buy HTTP wagon
 * - Convert the repository URL to "https://<domain>/
 */

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;

import java.util.Properties;

public class LeinGitlabWagon extends HttpWagon {

    private static String GITLAB_SCHEME = "gitlab:";
    private static String REPOSITORY_SCHEME = "https:";

    /**
     * If the repository URL scheme is "gitlab":
     *
     * Override so we can extract the token name and token from the AuthenticationInfo, and use them instead as HTTP Headers.
     *
     * Always pass null up to super class method for the AuthenticationInfo, as we don't want any HTTP Authentication performed.
     *
     * If the repository URL scheme is not "gitlab" just a straight pass through to the existing HttpWagon behaviour.
     *
     * If configured correctly in components.xml, this should only used for "gitlab" schemes.
     *
     * @param repository            pass through
     * @param authenticationInfo    Used to extract token name (username) and token (password or passphrase)
     * @param proxyInfoProvider     pass through
     * @throws ConnectionException
     * @throws AuthenticationException
     */
    public void connect(Repository repository, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider )
            throws ConnectionException, AuthenticationException {

        if (repository.getUrl().startsWith(GITLAB_SCHEME)) {
            repository.setUrl(correctUrl(repository));
            setHttpHeadersForAuthentication(authenticationInfo);
            super.connect(repository, null, proxyInfoProvider);
        }
        else {
            super.connect(repository, authenticationInfo, proxyInfoProvider);
        }
    }

    /**
     * Adjust the repository URL to convert the "gitlab" scheme to "https"
     * @param repository Repository as source of URL
     * @return The new corrected URL as a string.
     */
    private String correctUrl(Repository repository) {
        return repository.getUrl().replace(GITLAB_SCHEME, REPOSITORY_SCHEME);
    }

    /**
     * Use the username and password in the given authentication info to set a HTTP header (username) and value (password)
     * to be used om subsequent repository HTTP requests.
     *
     * If not username defined, it will default to "Private-Token"
     *
     * @param authenticationInfo   Contains username and password to be used for HTTP headers.
     * @throws AuthenticationException If no valid authentication information found for setting headers.
     */
    private void setHttpHeadersForAuthentication(AuthenticationInfo authenticationInfo) throws AuthenticationException {
        Properties headerProperties = new Properties();
        String tokenName = (authenticationInfo.getUserName() != null) ? authenticationInfo.getUserName() : "Private-Token";
        String token = (authenticationInfo.getPassword() != null) ? authenticationInfo.getPassword() : authenticationInfo.getPassphrase();

        if (token == null || token.isEmpty()) {
            throw new AuthenticationException("No password or passphrase defined for GitLab token");
        }

        headerProperties.setProperty(tokenName, token);
        setHttpHeaders(headerProperties);
    }

}
