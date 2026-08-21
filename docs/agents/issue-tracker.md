# Issue tracker: Local Markdown

Issues and specs for this repo live as Markdown files in `.scratch/`.

## Conventions

- One feature per directory: `.scratch/<feature-slug>/`
- The spec is `.scratch/<feature-slug>/spec.md`
- Implementation issues live at `.scratch/<feature-slug>/issues/<NN>-<slug>.md`
- Each issue contains a `Status:` line
- Comments are appended under `## Comments`
- Dependencies are recorded with `Blocked by: NN, NN`
- An issue is ready when it is open, unclaimed, and all blockers are resolved

## Skill operations

When a skill publishes a spec or issue, it creates the corresponding file under `.scratch/<feature-slug>/`.

When a skill fetches a ticket, it reads the referenced issue file.

Wayfinder maps use `.scratch/<effort>/map.md`; child decisions use numbered files under `.scratch/<effort>/issues/`.
