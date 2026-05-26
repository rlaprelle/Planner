# Releasing Echel Planner

Echel Planner follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Releases are cut from `main` after `dev` has been merged in. Tags live on `main` only.

## Versioning

- `MAJOR.MINOR.PATCH` (e.g. `0.1.0`).
- While the project is in `0.x`, the public API and data model may shift between minor versions. Once we commit to compatibility, bump to `1.0.0`.
- `backend/pom.xml` and `frontend/package.json` are version-locked — bump them together. There is no separate "snapshot" suffix during development; `dev` always carries the version of the next release.

## Release procedure

1. **Update `CHANGELOG.md`.** Move everything under `[Unreleased]` into a new `[X.Y.Z] - YYYY-MM-DD` section. Add the compare/tag link references at the bottom of the file and update the `[Unreleased]` link to point at the new tag.
2. **Bump versions** in `backend/pom.xml` (`<version>`) and `frontend/package.json` (`"version"`). They must match.
3. **Commit on `dev`:**
   ```powershell
   git add CHANGELOG.md backend/pom.xml frontend/package.json
   git commit -m "chore: release vX.Y.Z"
   ```
4. **Open a PR `dev` → `main` and merge it** once green.
5. **Tag on `main`:**
   ```powershell
   git checkout main
   git pull
   git tag -a vX.Y.Z -m "Release vX.Y.Z"
   git push origin vX.Y.Z
   ```
6. **(Optional) Publish a GitHub Release** with the changelog section as the body:
   ```powershell
   gh release create vX.Y.Z --title "vX.Y.Z" --notes-file -
   ```
   Pipe in just the new section of `CHANGELOG.md` (everything between the new heading and the next `## [` heading).

## After the release

- Confirm the tag is visible at https://github.com/rlaprelle/Planner/tags.
- Leave `dev` and `main` on `X.Y.Z` until the next release commit bumps them again.
