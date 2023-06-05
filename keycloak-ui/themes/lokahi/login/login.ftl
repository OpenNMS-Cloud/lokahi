<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
    <#if section = "header">
        <div class="typography-headline1 hs-sign-in">${msg("loginAccountTitle")}</div>
    <#elseif section = "form">
        <div id="hs-form">
            <div id="hs-form-wrapper">
                <#if realm.password>
                    <form autocomplete="off" id="hs-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">

                        <#if messagesPerField.existsError('username','password')>
                            <span id="hs-input-error" aria-live="polite">
                                    ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                            </span>
                        </#if>

                        <#if !usernameHidden??>
                            <input tabindex="1" placeholder="${msg('username')}" id="username" class="feather-input" name="username" value="${(login.username!'')}"  type="text" autofocus autocomplete="new-username"
                                aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                            />
                        </#if>

                        <input tabindex="2" placeholder="${msg('password')}" id="password" class="feather-input" name="password" type="password" autocomplete="new-password"
                                aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                        />

                        <#if usernameHidden?? && messagesPerField.existsError('username','password')>
                            <span id="input-error" aria-live="polite">
                                    ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                            </span>
                        </#if>

                        <div id="hs-form-options">
                            <#if realm.rememberMe && !usernameHidden??>
                                <div id="hs-remember-me" class="typography-body-small">
                                    <label>
                                        <#if login.rememberMe??>
                                            <input tabindex="3" id="rememberMe" class="hs-remember-me-checkbox" name="rememberMe" type="checkbox" checked> 
                                            <span class="hs-remember-me-text"> ${msg("rememberMe")}
                                        <#else>
                                            <input tabindex="3" id="rememberMe" class="hs-remember-me-checkbox" name="rememberMe" type="checkbox">
                                            <span class="hs-remember-me-text"> ${msg("rememberMe")} </span>
                                        </#if>
                                    </label>
                                </div>
                            </#if>
                        </div>
                        
                        <div id="hs-form-buttons" class="hs-sign-in-btn">
                            <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                            <button tabindex="4" class="btn hover focus btn-primary" name="login" id="hs-login" type="submit" value="${msg('doLogIn')}">Sign In</button>
                        </div>
                        <div class="hs-forgot-password-msg">
                            <#if realm.resetPasswordAllowed>
                                <span><a class="link typography-body-small" tabindex="5" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a></span>
                            </#if>
                        </div>

                        <div class="hs-footer">
                            <span>
                                <span class="hs-terms">Terms of Service</span>&nbsp;and&nbsp;<span class="hs-privacy">Privacy Policy</span>  
                            </span>
                        </div>
                    </form>
                </#if>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
