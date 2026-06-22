package afam.artidserver.config;

import afam.artidserver.model.VISIBILITY_STATE;
import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import java.sql.SQLException;
import java.util.List;

/**
 * Mappa l'enum Java {@link VISIBILITY_STATE} sull'omonimo enum Postgres della colonna
 * artid.visibility_state. Serve un converter dedicato perché di default Spring Data JDBC
 * mapperebbe le costanti per name() (PUBLIC/PRIVATE/...), mentre i label nel DB sono lowercase
 * (public/private/...); in più Postgres rifiuta una stringa "nuda" su una colonna enum, quindi
 * in scrittura si passa un PGobject tipizzato.
 */
@Configuration
public class JdbcConfig extends AbstractJdbcConfiguration {

    @Override
    protected List<?> userConverters() {
        return List.of(new VisibilityStateWritingConverter(), new VisibilityStateReadingConverter());
    }

    @WritingConverter
    static class VisibilityStateWritingConverter implements Converter<VISIBILITY_STATE, PGobject> {
        @Override
        public PGobject convert(VISIBILITY_STATE source) {
            PGobject pgObject = new PGobject();
            pgObject.setType("visibility_state");
            try {
                pgObject.setValue(source.getLabel());
            } catch (SQLException e) {
                throw new IllegalStateException("Impossibile serializzare visibility_state", e);
            }
            return pgObject;
        }
    }

    @ReadingConverter
    static class VisibilityStateReadingConverter implements Converter<String, VISIBILITY_STATE> {
        @Override
        public VISIBILITY_STATE convert(String source) {
            return VISIBILITY_STATE.fromLabel(source);
        }
    }
}
