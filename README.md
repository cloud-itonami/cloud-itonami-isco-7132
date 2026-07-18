# cloud-itonami-isco-7132

Open Occupation Blueprint for **ISCO-08 7132**: Spray Painters and Varnishers.

This repository designs a forkable OSS business for a spray-shop job scheduling and logistics coordination practice: a shop scheduling/logistics coordination robot manages crew/task records under a governor-gated actor, so a spray-coating shop keeps its own operating records instead of renting a closed workforce-management SaaS.

**Maturity: `:implemented`.** `src/sprayshop/` implements the
`SprayShopActor` as a `langgraph.graph/state-graph`
(`sprayshop.actor`) wired to a `Spray Shop Advisor`
(`sprayshop.advisor`) and an independent `SprayShopGovernor`
(`sprayshop.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. HARD invariants (always hold, never overridable):
sprayer provenance, shop provenance, no-actuation (`:effect` must be
`:propose`), a closed op-allowlist (`:log-work-record`,
`:schedule-crew-operation`, `:flag-safety-concern`,
`:coordinate-supply-order` — nothing else may ever be proposed), and a
permanent, unconditional block on any proposal that would directly
finalize a spray-application-execution decision (e.g. a specific
spray-coating or spray-finish application) or override a shop safety
officer's judgment. Always-escalate paths (human sign-off regardless
of confidence, mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (always) and `:coordinate-supply-order` above
the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a shop scheduling/logistics coordination robot performs crew scheduling, task/materials-usage/progress-record logging and spray-coating-materials supply-order coordination for a spray-coating shop, under an actor that proposes actions and an independent **Spray Shop Governor** that gates them. The governor never
dispatches hardware itself, never performs spray-painting work in the shop, and never finalizes a spray-application-execution decision or overrides a shop safety officer's judgment; `:high`/`:safety-critical` actions (such as a flagged fume-exposure/ventilation/flammable-material concern, or an above-threshold supply order) require human sign-off. **This actor coordinates SHOP SCHEDULING/LOGISTICS ONLY — it never performs spray-painting work itself.**

## Core Contract

```text
crew roster + shop registration + safety-reporting policy
        |
        v
Spray Shop Advisor -> Spray Shop Governor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, finalize
a spray-application-execution decision, override a shop safety officer's judgment,
suppress an operating record, or disclose sensitive data without governor
approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7132`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
