"""Single source of truth for concept-level production semantic approval."""

APPROVED_SEMANTIC_WORDS = {
    "peine", "entraîner", "complémentaire", "disposer", "trier", "rater", "scolaire",
    "province", "former", "convenir", "fondre", "reporter", "surnommer", "verger",
    "licencier", "four", "taper", "citadin", "geste", "recette", "lier", "stage", "car",
    "sort", "résumer", "toile", "tromper", "mince", "pair", "instance", "or", "encore",
    "coin", "cap", "somme", "chat", "ressentir", "caution", "hâter", "rentable", "hardi",
    "vaisselle", "récipient", "désigner", "fin", "joli", "don", "singe", "supplier",
    "doter", "solde", "quart", "lecteur", "tirer", "remarquer",
}

T2_TARGET_WORD = "remarquer"
T2_SENTENCE = "La police a **remarqué** un homme suspect."
T2_OPTIONS = (
    ("A", "repérer", True, "CORRECT"),
    ("B", "dire", False, "TRANSFER"),
    ("C", "classer", False, "DISTRACTOR"),
    ("D", "licencier", False, "DISTRACTOR"),
)
