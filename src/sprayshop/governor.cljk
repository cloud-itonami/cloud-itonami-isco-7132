(ns sprayshop.governor
  "SprayShopGovernor — the independent safety/scope layer gating every
  shop scheduling/logistics proposal an advisor may make for a spray
  shop. The governor never dispatches hardware itself, never performs
  spray-application work in the shop, and never finalizes a
  spray-application-execution decision (e.g. deciding to proceed with
  a specific spray-coating or spray-finish application) or overrides a
  shop safety officer's judgment — those are permanently out of this
  actor's scope and remain a shop safety officer's exclusive judgment
  (README's 'Robotics premise': this actor coordinates SHOP
  SCHEDULING/LOGISTICS ONLY — it never performs spray-painting work
  itself). Modeled on cloud-itonami-isco-7131's paintcrew.governor (the
  closely-related coating-application safety pattern).

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. sprayer provenance     — the crew member must be independently
                                verified/registered before any action.
    2. shop provenance        — the spray shop must be independently
                                verified/registered before any action.
    3. no-actuation            — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never performs spray-application work
                                itself; it only gates what the advisor
                                may coordinate).
    4. closed op-allowlist     — only :log-work-record,
                                :schedule-crew-operation,
                                :flag-safety-concern and
                                :coordinate-supply-order may ever be
                                proposed; anything else is refused.
    5. scope-excluded action   — any proposal to directly finalize a
                                spray-application-execution decision
                                (e.g. deciding to proceed with a
                                specific spray-coating or spray-finish
                                application), or to override a shop
                                safety officer's judgment, is a hard,
                                permanent block (checked both against
                                the proposed :op and, defense-in-depth,
                                against the proposal's :rationale text
                                — matched as full finalization/execution
                                ACTION phrases such as \"proceed with
                                the spray application\" / \"authorize
                                the spray application\" / \"override the
                                shop safety officer's judgment\", never
                                as bare nouns like \"spray\",
                                \"coating\" or \"safety\", so the check
                                can never self-trip on the advisor's
                                own routine rationale text, e.g.
                                \"logged work record for sprayer …\" or
                                \"scheduled crew operation for
                                spray-coating task …\" or \"…routed for
                                shop safety officer review\" — all three
                                legitimately contain those bare nouns
                                but none is a finalization action, and
                                all are exercised by
                                `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off
  regardless of confidence):
    6. :op :flag-safety-concern (a fume-exposure / inadequate-ventilation
                                / flammable-material-risk concern always
                                escalates to a human, never
                                auto-commits).
    7. :op :coordinate-supply-order above `supply-cost-threshold`.
    8. low confidence (< `confidence-floor`)."
  (:require [kotoba.lang.text :as str]
            [sprayshop.store :as store]))

(def confidence-floor 0.6)
(def supply-cost-threshold 2000)

(def allowed-ops
  #{:log-work-record :schedule-crew-operation
    :flag-safety-concern :coordinate-supply-order})

;; Defense-in-depth: none of these ops are ever in `allowed-ops`
;; above, so they are already refused by the closed-allowlist check
;; below; they are named again here — as explicit finalization/
;; execution ACTIONS, never bare nouns — so a future allowlist edit
;; cannot silently re-open this specific out-of-scope path without
;; also touching this list. This actor NEVER finalizes a
;; spray-application-execution decision and NEVER overrides a shop
;; safety officer's judgment — these ops are always a hard permanent
;; block, never auto-commit-eligible, never merely escalate-eligible.
(def ^:private scope-excluded-ops
  #{:finalize-spray-application-decision :authorize-spray-application
    :proceed-with-spray-application :override-shop-safety-officer-judgment})

;; Full finalization/execution ACTION phrases only — never bare nouns
;; ("spray", "coating", "finish", "safety") — so this can never match
;; inside the mock advisor's own default rationale text (which
;; legitimately contains those bare nouns, e.g. "spray-coating task" /
;; "shop safety officer review"). See
;; `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
(def ^:private scope-excluded-phrases
  ["proceed with the spray application" "finalize the spray application decision"
   "authorize the spray application" "authorize the spray-coating application"
   "override the shop safety officer's judgment"
   "override shop safety officer judgment"])

(defn- contains-excluded-phrase? [s]
  (let [s (str/lower (or s ""))]
    (boolean (some #(str/includes? s %) scope-excluded-phrases))))

(defn- hard-violations [proposal sprayer-record shop-record]
  (let [{:keys [op rationale]} proposal]
    (cond-> []
      (nil? sprayer-record)
      (conj {:rule :no-sprayer
             :detail "未登録 sprayer への提案は不可（sprayer record は独立して検証・登録済みでなければならない）"})

      (nil? shop-record)
      (conj {:rule :no-shop
             :detail "未登録 shop への提案は不可（shop record は独立して検証・登録済みでなければならない）"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は現場作業を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :unknown-op
             :detail (str op " は closed op-allowlist に無い — 提案不可")})

      (or (contains? scope-excluded-ops op) (contains-excluded-phrase? rationale))
      (conj {:rule :scope-excluded-action
             :detail "スプレー塗装作業の実行判断（吹き付け仕上げ/コーティング適用等）の確定・shop safety officer の判断の上書きは、この actor の権限外 — 常に永続ブロック"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `sprayshop.store/Store`. Pure — never mutates
  the store, never dispatches a shop operation."
  [request _context proposal store]
  (let [sprayer-record (store/sprayer store (:sprayer-id request))
        shop-record (some->> (:shop-id proposal) (store/shop store))
        hard (hard-violations proposal sprayer-record shop-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        supply-order-over-threshold?
        (and (= :coordinate-supply-order (:op proposal))
             (number? (:cost proposal))
             (> (:cost proposal) supply-cost-threshold))
        always-risky? (or (= :flag-safety-concern (:op proposal))
                           supply-order-over-threshold?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
