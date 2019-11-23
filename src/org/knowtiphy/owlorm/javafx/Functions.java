package org.knowtiphy.owlorm.javafx;

import java.time.LocalDate;
import java.util.function.Function;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Statement;
import org.knowtiphy.utils.JenaUtils;

public class Functions
{
    public static Function<Statement, Boolean> STMT_TO_BOOL = s -> JenaUtils.getB(s.getObject());
    public static Function<Statement, LocalDate> STMT_TO_DATE = s -> JenaUtils.fromDate((XSDDateTime)  s.getObject().asLiteral().getValue());
    public static Function<Statement, Integer> STMT_TO_INT = s -> JenaUtils.getI(s.getObject());
    public static Function<Statement, String> STMT_TO_STRING = s -> JenaUtils.getS(s.getObject());
}
