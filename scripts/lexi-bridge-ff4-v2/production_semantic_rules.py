"""Single source of truth for concept-level production semantic approval."""

RULESET_VERSION = "FF4_0811_CLIENT_REV2_20260812"
MINIMUM_T2_ITEM_COUNT = 16
MAX_CONSECUTIVE_FALSE_FRIEND_T3 = 3

APPROVED_SEMANTIC_WORDS = {
    "peine", "entraîner", "complémentaire", "disposer", "trier", "rater", "scolaire",
    "province", "former", "convenir", "fondre", "reporter", "surnommer", "verger",
    "licencier", "four", "taper", "citadin", "geste", "recette", "lier", "stage", "car",
    "sort", "résumer", "toile", "tromper", "mince", "pair", "instance", "or", "encore",
    "coin", "cap", "somme", "chat", "ressentir", "caution", "hâter", "rentable", "hardi",
    "vaisselle", "récipient", "désigner", "fin", "joli", "don", "singe", "supplier",
    "doter", "solde", "quart", "lecteur", "tirer", "remarquer",
}

T2_RULES = {
    "remarquer": {
        "sentence": "La police a **remarqué** un homme suspect.",
        "options": (
            ("A", "repérer", True, "CORRECT"),
            ("B", "dire", False, "TRANSFER"),
            ("C", "classer", False, "DISTRACTOR"),
            ("D", "licencier", False, "DISTRACTOR"),
        ),
    },
    "peine": {
        "sentence": "Ça **vaut la peine** d'aller visiter ce musée.",
        "options": (
            ("A", "vaut le coup", True, "CORRECT"),
            ("B", "fait mal", False, "TRANSFER"),
            ("C", "reste possible", False, "DISTRACTOR"),
            ("D", "semble facile", False, "DISTRACTOR"),
        ),
    },
    "désigner": {
        "sentence": "Que **désigne** ce mot dans le texte ?",
        "options": (
            ("A", "signifie", True, "CORRECT"),
            ("B", "dessine", False, "TRANSFER"),
            ("C", "prononce", False, "DISTRACTOR"),
            ("D", "efface", False, "DISTRACTOR"),
        ),
    },
    "supplier": {
        "sentence": "Je t'en **supplie**.",
        "options": (
            ("A", "prie", True, "CORRECT"),
            ("B", "fournis", False, "TRANSFER"),
            ("C", "parle", False, "DISTRACTOR"),
            ("D", "veux", False, "DISTRACTOR"),
        ),
    },
    "tromper": {
        "sentence": "Sa vue le **trompe** souvent.",
        "options": (
            ("A", "induit en erreur", True, "CORRECT"),
            ("B", "surpasse", False, "TRANSFER"),
            ("C", "protège", False, "DISTRACTOR"),
            ("D", "rassure", False, "DISTRACTOR"),
        ),
    },
    "coin": {
        "sentence": "Il habite au **coin** de la rue.",
        "options": (
            ("A", "angle", True, "CORRECT"),
            ("B", "pièce", False, "TRANSFER"),
            ("C", "centre", False, "DISTRACTOR"),
            ("D", "sommet", False, "DISTRACTOR"),
        ),
        "evidenceLevel": "TEM4_COLLOCATION_CONTEXTUALIZED",
        "sourceCollocation": "au coin de la rue",
    },
    "convenir": {
        "sentence": "Cette date me **convient**.",
        "options": (
            ("A", "va", True, "CORRECT"),
            ("B", "réunit", False, "TRANSFER"),
            ("C", "surprend", False, "DISTRACTOR"),
            ("D", "retarde", False, "DISTRACTOR"),
        ),
        "evidenceLevel": "TEM4_COLLOCATION_CONTEXTUALIZED",
        "sourceCollocation": "convenir à",
    },
    "don": {
        "sentence": "Elle a un **don** pour la musique.",
        "options": (
            ("A", "talent", True, "CORRECT"),
            ("B", "seigneur", False, "TRANSFER"),
            ("C", "doute", False, "DISTRACTOR"),
            ("D", "goût", False, "DISTRACTOR"),
        ),
        "evidenceLevel": "TEM4_COLLOCATION_CONTEXTUALIZED",
        "sourceCollocation": "avoir un don pour la musique",
    },
    "joli": {
        "sentence": "C'est une **jolie** petite fille.",
        "options": (
            ("A", "belle", True, "CORRECT"),
            ("B", "joyeuse", False, "TRANSFER"),
            ("C", "timide", False, "DISTRACTOR"),
            ("D", "grande", False, "DISTRACTOR"),
        ),
        "evidenceLevel": "TEM4_COLLOCATION_CONTEXTUALIZED",
        "sourceCollocation": "jolie petite fille",
    },
    "pair": {
        "sentence": "Cet artiste est sans **pair**.",
        "options": (
            ("A", "égal", True, "CORRECT"),
            ("B", "paire", False, "TRANSFER"),
            ("C", "public", False, "DISTRACTOR"),
            ("D", "avenir", False, "DISTRACTOR"),
        ),
        "evidenceLevel": "TEM4_COLLOCATION_CONTEXTUALIZED",
        "sourceCollocation": "sans pair",
    },
    "province": {
        "sentence": "Ils ont acheté une maison en **province**.",
        "options": (
            ("A", "région", True, "CORRECT"),
            ("B", "Provence", False, "TRANSFER"),
            ("C", "banlieue", False, "DISTRACTOR"),
            ("D", "montagne", False, "DISTRACTOR"),
        ),
        "evidenceLevel": "TEM4_COLLOCATION_CONTEXTUALIZED",
        "sourceCollocation": "acheter une maison en province",
    },
    "recette": {
        "sentence": "Il cherche une **recette** pour réussir.",
        "options": (
            ("A", "méthode", True, "CORRECT"),
            ("B", "reçu", False, "TRANSFER"),
            ("C", "question", False, "DISTRACTOR"),
            ("D", "chance", False, "DISTRACTOR"),
        ),
        "evidenceLevel": "TEM4_COLLOCATION_CONTEXTUALIZED",
        "sourceCollocation": "recette pour réussir",
    },
    "reporter": {
        "sentence": "Ils ont **reporté** la cérémonie.",
        "options": (
            ("A", "remis", True, "CORRECT"),
            ("B", "rapporté", False, "TRANSFER"),
            ("C", "annoncé", False, "DISTRACTOR"),
            ("D", "préparé", False, "DISTRACTOR"),
        ),
        "evidenceLevel": "TEM4_COLLOCATION_CONTEXTUALIZED",
        "sourceCollocation": "reporter la cérémonie = remettre",
    },
    "résumer": {
        "sentence": "Elle **résume** une histoire.",
        "options": (
            ("A", "synthétise", True, "CORRECT"),
            ("B", "reprend", False, "TRANSFER"),
            ("C", "invente", False, "DISTRACTOR"),
            ("D", "traduit", False, "DISTRACTOR"),
        ),
        "evidenceLevel": "TEM4_COLLOCATION_CONTEXTUALIZED",
        "sourceCollocation": "résumer une histoire",
    },
    "sort": {
        "sentence": "Il est maître de son **sort**.",
        "options": (
            ("A", "destin", True, "CORRECT"),
            ("B", "type", False, "TRANSFER"),
            ("C", "secret", False, "DISTRACTOR"),
            ("D", "choix", False, "DISTRACTOR"),
        ),
        "evidenceLevel": "TEM4_COLLOCATION_CONTEXTUALIZED",
        "sourceCollocation": "être maître du sort de quelqu'un",
    },
    "toile": {
        "sentence": "Il admire une **toile** de Cézanne.",
        "options": (
            ("A", "tableau", True, "CORRECT"),
            ("B", "labeur", False, "TRANSFER"),
            ("C", "sculpture", False, "DISTRACTOR"),
            ("D", "photo", False, "DISTRACTOR"),
        ),
        "evidenceLevel": "TEM4_COLLOCATION_CONTEXTUALIZED",
        "sourceCollocation": "toile de Cézanne",
    },
}

T2_TARGET_WORDS = tuple(T2_RULES)

# Same-form, same-core-meaning English/French controls requested by the client.
# These are deliberately outside the false-friend semantic pool and are used
# only as Vrai controls in Type 3.
TRUE_COGNATE_CONTROLS = {
    "science": "科学",
    "villa": "别墅",
    "radio": "收音机",
    "flatter": "奉承",
    "dragon": "龙",
    "opinion": "意见",
    "site": "网站",
    "standard": "标准",
    "contact": "联系",
    "ski": "滑雪；滑雪板",
}
