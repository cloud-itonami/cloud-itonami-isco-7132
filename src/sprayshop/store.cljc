(ns sprayshop.store
  "SSoT for the ISCO-08 7132 spray-shop scheduling/logistics coordination
  actor (itonami actor pattern, ADR-2607121000 / CLAUDE.md Actors
  section; README's 'Robotics premise' — a shop scheduling/logistics
  coordination robot performs crew scheduling,
  task/materials-usage/progress-record logging and spray-coating-materials
  supply-order coordination for a spray shop under this
  advisor/governor pair, which never dispatches hardware itself, never
  performs spray-application work itself, and never finalizes a
  spray-application-execution decision or overrides a shop safety
  officer's judgment — those remain the shop safety officer's exclusive
  judgment). Modeled on cloud-itonami-isco-7131's paintcrew.store (the
  closely-related coating-application safety pattern).

  Domain:

    sprayer — a registered spray-shop crew member
              (:sprayer-id, :name)
    shop    — a registered spray shop {:shop-id :name
              :max-supply-cost number}. `:max-supply-cost` is an
              informational registered ceiling used only to decide
              whether a `:coordinate-supply-order` proposal escalates
              to human sign-off (the governor never blocks a
              within-threshold order outright; it only decides
              commit vs. escalate).
    record  — a committed operating record (a logged
              task/materials-usage/progress entry, a scheduled crew
              operation, a flagged safety concern, or a coordinated
              supply order) — written ONLY via commit-record!.
    ledger  — append-only audit trail, commit or hold.")

(defprotocol Store
  (sprayer [s sprayer-id])
  (shop [s shop-id])
  (records-of [s sprayer-id])
  (ledger [s])
  (register-sprayer! [s sprayer])
  (register-shop! [s shop])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (sprayer [_ sprayer-id] (get-in @a [:sprayers sprayer-id]))
  (shop [_ shop-id] (get-in @a [:shops shop-id]))
  (records-of [_ sprayer-id] (filter #(= sprayer-id (:sprayer-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-sprayer! [s p]
    (swap! a assoc-in [:sprayers (:sprayer-id p)] p) s)
  (register-shop! [s st]
    (swap! a assoc-in [:shops (:shop-id st)] st) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:sprayers {} :shops {} :records [] :ledger []}
                                    seed)))))
