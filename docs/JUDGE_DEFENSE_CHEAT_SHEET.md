# FreshRoute: Judge Q&A Defense Cheat Sheet

This document equips you with battle-tested, authoritative answers to the toughest questions judges, technical evaluators, and domain experts will ask during the evaluation.

---

### Q1: "Why did you build a custom FEFO heuristic instead of using a standard Linear Programming (LP) solver like Simplex or CPLEX?"
**Winning Answer:**
> *"Linear Programming is excellent for static, deterministic logistics, but perishable fresh produce operates under non-linear decay dynamics and sub-second operational constraints. Under the Arrhenius temperature decay model ($Q_{10}$), shelf life deteriorates exponentially when cold-chain failures occur. A heavy LP solver introduces multi-second latency and requires cloud dependencies. Our custom greedy FEFO-with-Marginal-Contribution heuristic executes in $O(N \log N + N \cdot M)$ time—under 15 milliseconds on Java 21—enabling real-time re-planning at the edge right when a refrigerated truck reports a delay or temperature spike."*

---

### Q2: "Doesn't dynamic discounting create price cannibalization—won't wholesale buyers wait for produce to decay to get the 35% discount?"
**Winning Answer:**
> *"We designed the dynamic discount model specifically to prevent buyer gaming:
> 1. **Destination Segmentation:** The 35% salvage markdown is **never offered to primary retail supermarkets** (who demand a strict $\ge 24\text{h}$ shelf-life buffer). It is exclusively routed to **secondary food processors and pulping plants** (e.g., tomato puree manufacturers) who process inventory immediately upon arrival.
> 2. **Algorithmic Allocation Locks:** Once a batch is allocated to a buyer at full price, inventory is locked using pessimistic database transactions (`@Lock(PESSIMISTIC_WRITE)`). Buyers cannot 'hold out' for price drops because batches that qualify for discounts are automatically re-assigned to processing clusters."*

---

### Q3: "What happens if rural collection centers lose internet connectivity? How does FreshRoute function offline?"
**Winning Answer:**
> *"This is one of the strongest reasons we selected our architecture:
> 1. **Lightweight Edge Deployment:** The Spring Boot 3 backend and embedded database run self-contained on local hub hardware without requiring an active internet connection.
> 2. **Local Decision Autonomy:** Rural packhouses can execute local FEFO dispatches and print consignment waybills offline.
> 3. **Eventual Consistency:** When connectivity resumes, transaction logs synchronize with the central cluster via idempotent REST endpoints, preventing double-allocation through unique UUID batch tracking."*

---

### Q4: "How did you validate your economic model and spoilage figures?"
**Winning Answer:**
> *"We grounded our cost model directly in the published **Ministry of Food Processing Industries (MoFPI) 2020–22 baseline report** on Indian post-harvest losses. We modeled:
> - True procurement basis: ₹18–25/kg across 5 core commodities.
> - Cold reefer freight costs: ₹1.80–₹3.20/kg depending on distance.
> - Spoilage disposal penalties: ₹3.00/kg municipal dump and handling fees.
> On our deterministic benchmark of 32 metric tonnes across 12 realistic batches, our baseline FIFO strategy experienced 18.1% spoilage (matching the national average for tomatoes and greens), while FreshRoute dropped spoilage to 5.0%, preserving ₹2.5 Lakhs in net revenue on a single distribution cycle."*

---

### Q5: "How does the backend ensure thread safety if multiple distribution managers allocate the same batch simultaneously?"
**Winning Answer:**
> *"We use **Pessimistic Row-Level Locking** (`SELECT ... FOR UPDATE` via Spring Data JPA) inside atomic `@Transactional` boundaries. When Hub Manager A initiates an allocation on Batch #TOM-104, that specific row is locked in the database. Hub Manager B's concurrent request is queued and receives the updated remaining quantity immediately after Manager A's commit, guaranteeing zero underflow or duplicate dispatch."*
