export default function Privacy() {
  return (
    <div className="container">
      <h1>
        Privacy Notice for Registration <span className="tag">6-Week MVP</span>
      </h1>

      <div className="sub">
        Applies to the demo of the Carbon Credit Exchange of{" "}
        <strong>[Platform/Company Name]</strong>. Last updated:{" "}
        <strong>[dd/mm/yyyy]</strong>.
      </div>

      <div className="card">
        <strong>Purpose of Processing</strong>
        <ul>
          <li>
            <b>Registration & Security:</b> identity verification (KYC), fraud
            prevention, 2FA.
          </li>
          <li>
            <b>Service Delivery:</b> order creation/matching, payment (DvP),
            registry transfer, issuance of <i>Retirement Certificate</i>, dispute
            resolution.
          </li>
          <li>
            <b>Legal Compliance:</b> KYC/AML/CTF, government/court requests,
            accounting & record keeping.
          </li>
          <li>
            <b>Minimal Improvement:</b> anonymized/pseudonymized statistics for
            operation & bug fixing.
          </li>
        </ul>

        <strong>Minimum Data Collected</strong>
        <ul>
          <li>
            <b>Identity/KYC:</b> full name, date of birth, nationality, address,
            ID documents & numbers, face photo/video.
          </li>
          <li>
            <b>Contact & Account:</b> email, phone number, login logs (IP,
            user-agent), security settings.
          </li>
          <li>
            <b>Transactions:</b> payment account (if required),
            orders/matching/settlement, transfer/retirement, EOR.
          </li>
          <li>
            <b>Technical:</b> cookies/session ID (essential only), system/error
            logs.
          </li>
        </ul>

        <strong>Automated Decision-Making in KYC</strong>
        <div className="small">
          The system may automatically match documents/photos. You have the
          right to request <b>manual review</b> if the outcome significantly
          affects your use of the service.
        </div>

        <strong>Data Retention</strong>
        <div className="small">
          Retain <b>only as necessary</b> for the stated purposes/legal
          obligations. MVP guideline: up to <b>2 years</b> after account
          deletion, longer if required by law or disputes. When expired: securely
          delete or isolate according to regulations.
        </div>

        <strong>Data Sharing</strong>
        <div className="small">
          Only with essential service providers
          (KYC/hosting/email/error-analytics/payment/customer support) under a
          confidentiality agreement; government/courts upon valid request.{" "}
          <b>No sale of personal data.</b>
        </div>

        <strong>Cross-Border Data Transfers</strong>
        <div className="small">
          Appropriate safeguards are applied (e.g., standard contractual clauses)
          when transferring data outside your country of residence.
        </div>

        <strong>Your Rights</strong>
        <div className="small">
          Access, rectification, erasure, restriction/objection to processing,
          data copy/portability, withdraw consent (if applicable). Contact{" "}
          <b>[privacy@companyname.com]</b>. Response time:{" "}
          <b>72 business hours</b>.
        </div>

        <div className="kv muted">
          <strong>Minimum Age:</strong> Service is only available for users{" "}
          <b>18+</b> (or legal age of majority in your country). If an underage
          account is detected, we will terminate and delete related data.
        </div>
      </div>

      <div className="card consents">
        <label>
          <input type="checkbox" required /> I confirm I am <b>18 years old</b>{" "}
          or older and have read the{" "}
          <a href="[terms-link]" target="_blank" rel="noopener noreferrer">
            Terms of Service
          </a>{" "}
          &amp;{" "}
          <a
            href="[privacy-policy-link]"
            target="_blank"
            rel="noopener noreferrer"
          >
            Privacy Policy
          </a>
          .
        </label>
        <label>
          <input type="checkbox" required /> I agree to allow{" "}
          <b>[Platform/Company Name]</b> to process <b>KYC/AML</b> data
          (including automated checks) to provide the service; I understand I
          have the right to request <b>manual review</b>.
        </label>
        <label>
          <input type="checkbox" /> I agree to receive product updates/newsletters
          (unsubscribe anytime).
        </label>
        <div className="muted small">
          Only <b>essential cookies</b> are used on the registration screen.
          Cookie settings can be changed in your browser.
        </div>
      </div>

      <div className="links small">
        Contact DPO/security lead:{" "}
        <a href="mailto:privacy@companyname.com">privacy@companyname.com</a> ·
        Sub-processor list:{" "}
        <a href="[subprocessor-link]" target="_blank" rel="noopener noreferrer">
          view here
        </a>
        .
      </div>

      <div className="footer">
        © {new Date().getFullYear()} [Platform/Company Name]. All rights
        reserved.
      </div>
    </div>
  );
}
