# OneSec Android 第一版

Status: ready-for-agent

## Problem Statement

使用者每天在短视频和游戏应用上花费过多时间。单纯提醒很容易被忽略，而完全不可解除的限制又可能在紧急情况下妨碍正常使用。使用者需要一套在冲动发生前预先设定、在额度耗尽后可靠执行、同时保留有限紧急出口的个人自律机制，并且敏感的应用使用记录不应离开手机。

## Solution

OneSec 是一款只面向 Android 的本地自律应用。使用者选择受限应用，为每个应用设置每日额度和限制等级；OneSec 统计应用在前台的实际使用时长，并在额度耗尽后执行强限制或弱限制。强限制持续到次日零点，但每天允许一次经过等待和原因记录的紧急解锁；弱限制允许经过等待获得短暂使用窗口。规则、历史和解锁原因全部保存在本机，核心权限失效时明确告知使用者保护已经失效。

## User Stories

1. As a OneSec user, I want to understand why usage-access permission is needed, so that I can make an informed decision before granting it.
2. As a OneSec user, I want to understand why accessibility permission is needed, so that I know how OneSec detects and intercepts a restricted app.
3. As a OneSec user, I want OneSec to detect whether each required permission is active, so that I can see whether protection is operational.
4. As a OneSec user, I want a prominent warning when a required permission is revoked, so that incomplete statistics are not presented as reliable.
5. As a OneSec user, I want guidance for ColorOS background-running and battery settings, so that protection remains reliable on my OPPO Reno.
6. As a OneSec user, I want to see installed applications, so that I can choose which applications OneSec should manage.
7. As a OneSec user, I want applications to be identified by their installed package and display name, so that rules continue to refer to the intended application.
8. As a OneSec user, I want to classify an application as a strong restriction, so that it is intercepted after its daily allowance is exhausted.
9. As a OneSec user, I want to classify an application as a soft restriction, so that extra use requires a deliberate pause.
10. As a OneSec user, I want to leave an application unrestricted, so that essential applications remain unaffected.
11. As a OneSec user, I want a default 30-minute daily allowance for a strong restriction, so that initial setup is quick.
12. As a OneSec user, I want a default 60-minute daily allowance for a soft restriction, so that initial setup is quick.
13. As a OneSec user, I want to configure each application's daily allowance independently, so that different applications can have different limits.
14. As a OneSec user, I want only foreground use to consume the daily allowance, so that background activity does not unfairly reduce it.
15. As a OneSec user, I want usage accumulation to stop when the application goes to the background, the phone locks, or a call interrupts me, so that recorded use reflects attention spent in the application.
16. As a OneSec user, I want allowances to reset at local midnight, so that each local calendar day starts with a fresh allowance.
17. As a OneSec user, I want usage history stored by local date, so that today's allowance is derived from auditable history rather than a fragile remaining-time counter.
18. As a OneSec user, I want to add a new restriction or tighten a rule immediately, so that a deliberate decision to be stricter takes effect now.
19. As a OneSec user, I want a relaxed allowance to become a pending relaxation until the next local midnight, so that an impulsive decision cannot weaken today's commitment.
20. As a OneSec user, I want removal of a strong restriction to become a pending relaxation, so that I cannot immediately escape an active commitment.
21. As a OneSec user, I want disabling protection to become a pending relaxation, so that protection cannot be casually switched off during temptation.
22. As a OneSec user, I want a restricted app to open normally while its daily allowance remains, so that OneSec does not create unnecessary friction.
23. As a OneSec user, I want a strong restriction to intercept an application when its daily allowance is exhausted, so that I stop using it for the rest of the day.
24. As a OneSec user, I want the intervention screen to show the application, today's use, and reset time, so that I understand why access was stopped.
25. As a OneSec user, I want an obvious way to return to the home screen from an intervention, so that I can immediately leave the restricted application.
26. As a OneSec user, I do not want an immediately visible “one more minute” action, so that the intervention does not encourage an impulsive override.
27. As a OneSec user, I want a soft restriction to require a 15-second wait after its daily allowance is exhausted, so that automatic opening becomes a conscious choice.
28. As a OneSec user, I want completion of the soft-restriction wait to grant a five-minute usage window, so that necessary additional use remains bounded.
29. As a OneSec user, I want another intervention when a usage window expires, so that temporary access does not silently become unlimited access.
30. As a OneSec user, I want one emergency override per local day for strong restrictions, so that an exceptional need can be handled without abandoning OneSec.
31. As a OneSec user, I want an emergency override to require a 60-second wait, so that the override is deliberate rather than impulsive.
32. As a OneSec user, I want to record a reason before an emergency override, so that I can review why I broke the original commitment.
33. As a OneSec user, I want an emergency override to grant only five minutes, so that exceptional access remains bounded.
34. As a OneSec user, I want OneSec to remember whether today's emergency override was used, so that a restart cannot create additional overrides.
35. As a OneSec user, I want a today overview of total restricted-app use, so that I can see my overall progress.
36. As a OneSec user, I want to see used time and remaining allowance for each restricted app, so that I can budget the rest of my day.
37. As a OneSec user, I want to see today's intervention count, so that I can recognize how often I attempted to exceed a rule.
38. As a OneSec user, I want to see whether today's emergency override has been used, so that its availability is unambiguous.
39. As a OneSec user, I want OneSec to restore rules and protection after the phone restarts, so that restarting does not silently bypass restrictions.
40. As a OneSec user, I want a quiet persistent notification while monitoring is active, so that Android and ColorOS can keep protection running without distracting me.
41. As a OneSec user, I want all rules and history to remain on my phone, so that sensitive usage data is private.
42. As a OneSec user, I want OneSec to operate without network permission, so that local-only behavior is technically enforceable.
43. As a OneSec user, I want usage, intervention, and emergency-override history retained for 90 days, so that I can evaluate whether OneSec is helping.
44. As a OneSec user, I want to clear all locally stored OneSec data, so that I remain in control of my information.
45. As a OneSec user, I want OneSec to acknowledge that Android allows permission revocation, force stop, and uninstall, so that the product does not promise impossible protection.
46. As a OneSec user, I want the first release validated on my OPPO Reno PCAM00 running Android 11 and ColorOS 11.1, so that the application solves my actual problem before expanding compatibility.

## Implementation Decisions

- The first release is a native Android application built with Kotlin, Jetpack Compose, and Material 3. It targets the OPPO Reno PCAM00 test device first and provisionally supports Android 10 and newer.
- OneSec is independently implemented. Existing open-source blockers may inform product and architecture choices, but GPL source is not copied or used as the basis of a derivative application.
- The restriction-decision module is the primary module and primary test seam. Its interface accepts the current local time, target restricted app, dated usage, active rule, pending relaxation, active usage window, emergency-override record, and protection-permission state. It returns an observable protection decision: allow, hard intervention, soft wait, temporary usage window, or protection unavailable.
- Restriction decisions contain no Android framework dependency. Android capabilities enter through adapters, keeping rule behavior deterministic and testable independently of a device.
- A usage adapter reads application foreground activity from Android usage events. Stored records remain dated facts; remaining allowance is derived rather than treated as authoritative mutable state.
- A foreground-detection adapter receives accessibility events and identifies the application the user is attempting to use. It does not inspect application-internal areas in the first release.
- An intervention adapter presents the full-screen intervention experience and offers a return-to-home action. Android platform restrictions must be handled honestly; OneSec does not claim absolute enforcement.
- A persistence adapter stores restricted apps, active rules, pending relaxations, dated usage, interventions, usage windows, emergency overrides, and retention metadata locally. Room is used for structured records and DataStore for small application settings.
- A clock adapter supplies the local date and time so that midnight, countdowns, test scenarios, and time changes are controllable at the restriction-decision seam.
- An application-catalog adapter lists installed applications with stable package identifiers and user-facing labels. Rules are configured data, never a hard-coded list of distracting applications.
- Strong restrictions default to 30 minutes per local day. Once exhausted, access is intercepted until the next local midnight except for the single emergency override.
- Soft restrictions default to 60 minutes per local day. After exhaustion, a 15-second wait grants a five-minute usage window; expiration returns the application to an intervention state.
- New restrictions and stricter changes may take effect immediately. Relaxed allowances, strong-restriction removal, and protection disabling are modeled as pending relaxations that take effect at the next local midnight.
- A strong restriction permits at most one emergency override per local day. The override requires a completed 60-second wait and a non-empty recorded reason, then creates a five-minute usage window.
- Permission health is part of protection state. If usage access or accessibility is missing, the application reports protection unavailable and does not present incomplete usage as trustworthy.
- A boot adapter restores monitoring after device restart. A quiet persistent notification and explicit ColorOS battery-optimization guidance support background reliability.
- The user interface comprises permission guidance, today overview, installed-application selection, rule editing, and intervention/emergency-override experiences.
- The today overview initially shows total restricted-app use, per-app used and remaining time, intervention count, and emergency-override status. Seven-day visualization is deferred until the core flow is reliable.
- The application requests no network permission and includes no accounts, analytics, telemetry, remote server, or cloud synchronization. Local history is retained for 90 days and can be cleared by the user.
- Development installation uses Android Studio and USB debugging. Store distribution is not part of the first release.

## Testing Decisions

- Good tests assert behavior visible through a module's interface or to the user. They do not assert private functions, ViewModel structure, Room implementation details, or incidental Compose hierarchy.
- The restriction-decision module is the main test surface. Parameterized deterministic tests cover allowance boundaries, foreground-time totals, local-midnight reset, immediate tightening, pending relaxations, strong interventions, soft waits, usage-window expiry, emergency-override eligibility, and missing permissions.
- Tests supply a controllable clock and in-memory adapters where variation is real. Production adapters are substituted only at defined seams; internal implementation is not exposed merely to make tests convenient.
- Persistence contract tests verify that rules, dated usage, pending relaxations, windows, and emergency-override records survive process recreation and retain their domain meaning.
- Compose tests cover complete observable flows: permission state, application selection, rule configuration, overview state, intervention, soft wait, and emergency override. They interact through semantics exposed to a user rather than implementation identifiers where practical.
- Android integration tests verify usage-access detection, accessibility-event translation, application catalog behavior, intervention presentation, return-to-home behavior, boot restoration, notification state, and permission revocation.
- Manual acceptance on the OPPO Reno PCAM00 verifies ColorOS background survival, battery-optimization guidance, real foreground usage measurement, intervention timing, and recovery after a physical reboot.
- The first vertical acceptance scenario grants permissions, selects one installed app, stores a daily allowance, reads today's foreground use, intervenes when the allowance is exhausted, and restores the rule after reboot.
- Long-running acceptance consists of a 14-day personal trial. Success means restricted applications do not exceed configured allowances outside explicit windows, false interventions do not cause protection to be disabled or OneSec to be uninstalled, and emergency overrides average no more than two per week.
- There is no prior test suite in the repository. The restriction-decision seam established by this spec becomes the prior art for later rules and platform adapters.

## Out of Scope

- iPhone or other non-Android platforms.
- Accounts, network services, cloud synchronization, remote management, analytics, or telemetry.
- Detecting or blocking application-internal areas such as Shorts, Reels, video feeds, comments, or chat sections.
- Social features, leaderboards, achievements, shared challenges, or gamification.
- Parent/child accounts, family control, administrator-managed devices, or device-owner provisioning.
- Guaranteed prevention of permission revocation, force stop, safe mode, uninstall, ADB actions, or factory reset.
- Website-level blocking, VPN-based filtering, DNS filtering, or browser URL inspection.
- Application-store publication, subscriptions, payments, signing/release operations, or store-policy approval.
- Seven-day charts in the initial core vertical slice.
- Supporting or optimizing for devices other than the OPPO Reno PCAM00 before the first real-device flow is reliable.

## Further Notes

- The canonical domain vocabulary is defined in the OneSec glossary. Specifications, tickets, tests, and user-facing discussions should use terms such as restricted app, daily allowance, strong restriction, soft restriction, usage window, emergency override, pending relaxation, and intervention consistently.
- The first implementation should remain a tracer bullet through the real Android stack rather than building every screen or rule in isolation. Platform feasibility and ColorOS reliability are the highest early risks.
- The Android platform lets a device owner revoke permissions, force stop, or uninstall an ordinary application. OneSec provides deliberate friction and honest status reporting, not an impossible guarantee of non-bypassability.
- Any future proposal to add networking, derive code from a GPL project, or support a cross-platform framework must explicitly revisit the existing architectural decisions.

## Comments

