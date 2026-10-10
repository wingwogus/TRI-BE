# Git Flow Runbook

This is the canonical branch, PR, review, release, and hotfix policy for Tribe. `AGENTS.md` contains only the mandatory pre-edit checks; GitHub settings and CI enforce the parts that documentation cannot.

## Before Writing

Check `git branch --show-current`, `git status --short --branch`, and the worktree list. Do not edit directly on `main` or `dev`, and do not overwrite or move another person's uncommitted work. Read-only investigation needs no branch. If the user explicitly chooses another branch or worktree, follow that instruction after reporting the risk.

## Branches

| Branch | Purpose | Start from | PR target |
| --- | --- | --- | --- |
| `main` | Always deployable production state | Release or hotfix PR only | — |
| `dev` | Integration branch for the next release | Approved work PRs and post-hotfix sync | `main` at release |
| `feat/{issue}-{name}` | New feature | Latest `dev` | `dev` |
| `fix/{issue}-{name}` | Ordinary bug fix | Latest `dev` | `dev` |
| `chore/{name}` | Configuration, dependencies, CI/CD, or maintenance | Latest `dev` | `dev` |
| `hotfix/{issue}-{name}` | Urgent production repair | Latest `main` | `main`, then sync to `dev` |

Use a descriptive name when there is no issue number. Examples: `feat/123-kakao-login`, `fix/145-payment-validation`, `hotfix/152-login-outage`, `chore/update-gradle`.

Keep work branches short-lived. For a long feature, prefer small PRs behind a feature flag when one is available; this repository does not currently define a shared feature-flag system.

## Normal Development and Commits

1. Update local `dev` from the reviewed upstream state, then create the appropriate work branch. If local `dev` has unpublished commits, resolve that divergence before treating it as the shared starting point.
2. Make small, reviewable changes and run the relevant checks in `docs/runbooks/local-harness.md`.
3. Open a PR to `dev` using `.github/pull_request_template.md`. Do not push directly to `dev` or `main`.
4. Merge only after required CI, reviews, and conversation resolution pass. Squash Merge ordinary `feat/*`, `fix/*`, and `chore/*` PRs, then delete the remote work branch.

Commit subjects and PR titles intended for Squash Merge use Conventional Commits: `type(scope): 제목`. Allowed types are `feat`, `fix`, `docs`, `refactor`, `test`, and `chore`. Examples: `feat(auth): 카카오 로그인 구현`, `fix(trip): 일정 삭제 권한 검증 수정`, `chore(ci): 백엔드 테스트 워크플로 수정`.

For decision-heavy commits, an optional body or Git trailers may record constraints, rejected alternatives, test evidence, and residual risk. Do not use trailers to replace the PR verification record.

## Reviews

General changes require at least one approval, passing required CI, and resolution of all review conversations. High-risk changes require at least two approvals, including a relevant area owner or CODEOWNER, plus the same CI and conversation requirements. Reviewers need not invent a question when they have no concern.

Treat authentication/authorization, administrator permissions, payment/settlement, database schema, personal data, external API contracts, production environment variables, CI/CD/deployment, secrets, and security changes as high-risk. A production release PR into `main` follows the high-risk review level.

Review for requirement coverage, auth/security, error handling, test coverage, regression risk, API/DB contracts, production impact, and rollback feasibility. Ask a question when clarification is genuinely needed; approval without a question is valid.

### Solo fork profile

The approval counts above apply to the shared upstream repository. A one-person fork cannot satisfy required independent approvals because the PR author cannot approve their own PR. In a personal fork:

- keep `main` and `dev` protected from direct development and continue using `feat/*`, `fix/*`, `hotfix/*`, and `chore/*` branches;
- require a PR and the `pr-verify` status check, but set the required approval count to `0` unless another reviewer is actually available;
- perform a documented self-review using the PR checklist and resolve every conversation before merge;
- for high-risk changes, record security, migration, deployment, and rollback evidence explicitly and seek external review when practical, but do not claim independent approval occurred when it did not;
- keep `upstream` pointed at the shared repository and `origin` pointed at the personal fork. Sync reviewed upstream changes into the fork's `dev` before starting new work.

This solo profile relaxes reviewer count only. It does not relax CI, branch separation, verification, rollback, or secret-handling requirements.

## Release: `dev` → `main`

1. Complete integration tests on `dev` and open a release PR from `dev` to `main`.
2. Record included features and PRs/issues, test results, known risks, and rollback target. Explicitly check DB, environment-variable, and API contract changes.
3. Merge only after required CI and high-risk review requirements pass; do not push directly to `main`.
4. Create a version tag on the merged `main` commit. Tagging is a manual release step; no tag workflow exists here.
5. Verify the frontend Vercel release separately. The backend `main` push runs `.github/workflows/ci.yml`'s `build-and-deploy` job to build and push an image; `ops/argocd/tribe-api-prod.yaml` tracks `main` at `ops/helm/tribe-api`, with runtime values in `ops/helm/tribe-api/values.yaml`. Confirm the intended image tag and Argo CD sync rather than assuming either happened.
6. Run `docs/runbooks/deployment-smoke.md`. On failure, use the documented previous image tag/Helm values, Argo CD recovery action, or Vercel rollback target, and record the owner and result.

The backend image workflow, Argo CD sync, Vercel deployment, tag creation, and rollback are distinct steps. The repository does not contain an automated release-tag or rollback workflow.

## Hotfix: `main` → `hotfix/*` → `main` → `dev`

1. Branch from the latest `main`, make the smallest production repair, and run focused regression tests.
2. Open a `main`-targeted hotfix PR. Obtain high-risk approval and passing CI before merging and deploying.
3. Run production smoke checks and record the recovery/rollback path.
4. **Always bring the same fix back to `dev`** through a `main` → `dev` sync PR or a second PR from the hotfix branch. Do not leave the fix only on `main`, or the next release may reintroduce the outage. Do not make ad hoc cherry-picking the default procedure.

## PR Template and Ownership

Every PR records summary, related issue if any, verification and skipped checks, API/DB/environment contract changes, deployment impact, rollback, and residual risk. A release PR additionally lists included changes and the intended tag/deploy target.

There is no `.github/CODEOWNERS` file in this repository. Do not add placeholder accounts. Once the team supplies actual GitHub users or teams with write access, assign owners for `backend/`, `frontend/`, `ops/`, and `docs/contracts/`, then enable required CODEOWNER review in GitHub. Until then, the relevant area owner must be selected manually for high-risk PRs.

## Enforcement: GitHub Settings and CI

Repository files alone do **not** activate GitHub Rulesets or Branch Protection. An administrator must configure and verify these settings on GitHub. Apply protection to administrators too and avoid bypass actors that can directly push. Use Rulesets or Branch Protection without conflicting duplicate rules.

| Target | Required GitHub settings |
| --- | --- |
| `main` | PR required; two approvals for release/hotfix review; CODEOWNER review after real owners are defined; required `pr-verify` status; conversations resolved; force push and deletion disabled; no direct-push bypass. |
| `dev` | PR required; at least one approval; required `pr-verify` status; conversations resolved; force push and deletion disabled; no direct-push bypass. High-risk PRs still need a second approval and area owner. |

For a one-person fork, use the same branch protections but set required approvals to `0`; keep PR, `pr-verify`, conversation resolution, force-push prevention, and deletion prevention enabled.

The only PR validation workflow currently defined is `.github/workflows/ci.yml` job `pr-verify`, triggered for PRs targeting `dev` or `main`. It runs frontend lint/test/typecheck/build, backend tests, and changed-file whitespace checks. Configure `pr-verify` as a required status check **after** it has run on a PR. The same workflow's `build-and-deploy` job runs only on backend-related pushes to `main`; it is not a PR check. GitHub settings, not this YAML file, block merge on the required status.

GitHub's fixed approval count cannot express “one approval normally, two only for high-risk files” on the same `dev` branch by itself. Until a reliable conditional review check and real CODEOWNERS are installed, enforce the second high-risk approval during review; do not claim it is automated. See [GitHub protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches) and [ruleset options](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/available-rules-for-rulesets).

**Protection prerequisite:** `ops/image-updater/tribe-api-updater.yaml` currently uses Git write-back to `main` (`gitConfig.branch: main`). A no-bypass direct-push rule would reject that automation. Before enabling the `main` rule, replace its write-back with a reviewed PR-based promotion path or disable that direct write-back and manage image tags through release PRs. Do not grant the updater a silent direct-push bypass. This migration is not implemented by the current CI or Helm files.
