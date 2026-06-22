package afam.artidserver.model;

public enum VISIBILITY_STATE {

    PUBLIC("public"),
    PRIVATE("private"),
    UNLISTED("unlisted");

    private final String label;

    VISIBILITY_STATE(String label){
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    // Risale alla costante dal valore dell'enum Postgres (lowercase): le costanti Java sono
    // UPPERCASE (public/private sono keyword riservate) quindi name() non basta per il mapping.
    public static VISIBILITY_STATE fromLabel(String label) {
        for (VISIBILITY_STATE state : values()) {
            if (state.label.equals(label)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Stato di visibilità non riconosciuto: " + label);
    }
}
