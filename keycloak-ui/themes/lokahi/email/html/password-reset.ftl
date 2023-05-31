<#import "template.ftl" as layout>
<@layout.emailLayout>
  <p style="
      color: #273180; 
      font-weight: bold; 
      font-size: 17px;
    ">
    ${kcSanitize(msg("forgotPasswordText"))?no_esc}
  </p>
  <p>
    ${kcSanitize(msg("resetPasswordText", linkExpirationFormatter(linkExpiration)))?no_esc}
  </p>
  <div style="
        display: block;
        width: 100%;
      ">
    <a href="${link}" 
      style="
        text-decoration: none;
        display: block; 
        background: #273180; 
        color: white; 
        padding: 7px 15px; 
        border-radius: 5px; 
        font-weight: 600;
        width: 126px;
        margin: auto;
        margin-top: 40px;
      ">
      RESET PASSWORD
    </a>
  </div>
</@layout.emailLayout>
