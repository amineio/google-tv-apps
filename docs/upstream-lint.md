# Existing TV Home lint errors

The monorepo adds Android lint to CI for both apps.
The inherited home app contains 29 lint errors at upstream commit `6bcd649`.
All 63 home source files match the imported source before this build change.

The baseline at `apps/home/lint-baseline.xml` records only these existing errors:

| Lint rule | Count |
| --- | --- |
| MissingTranslation | 27 |
| CoarseFineLocation | 1 |
| ByteOrderMark | 1 |

The baseline does not fix these issues. It keeps them separate from new lint errors.
Warnings remain visible. New errors fail CI.
TV Clock has no lint baseline.

Resolve each inherited issue in a focused change with appropriate tests.
Remove its matching baseline entry after the fix passes lint.
Do not refresh the baseline to accept new errors.
