
package shardingsphere.workshop.parser.engine.visitor;

import autogen.MySQLStatementBaseVisitor;
import autogen.MySQLStatementParser.ColValueContext;
import autogen.MySQLStatementParser.ConditionContext;
import autogen.MySQLStatementParser.IdentifierContext;
import autogen.MySQLStatementParser.InsertContext;
import autogen.MySQLStatementParser.SchemaNameContext;
import autogen.MySQLStatementParser.SelectContext;
import autogen.MySQLStatementParser.TableNameContext;
import autogen.MySQLStatementParser.UseContext;
import shardingsphere.workshop.parser.statement.ASTNode;
import shardingsphere.workshop.parser.statement.segment.ColValueSegment;
import shardingsphere.workshop.parser.statement.segment.ConditionSegment;
import shardingsphere.workshop.parser.statement.segment.IdentifierSegment;
import shardingsphere.workshop.parser.statement.segment.SchemeNameSegment;
import shardingsphere.workshop.parser.statement.segment.SelectSegment;
import shardingsphere.workshop.parser.statement.statement.UseStatement;

/**
 * MySQL visitor.
 */
public final class SQLVisitor extends MySQLStatementBaseVisitor<ASTNode> {
    
    @Override
    public ASTNode visitUse(final UseContext ctx) {
        SchemeNameSegment schemeName = (SchemeNameSegment) visit(ctx.schemaName());
        return new UseStatement(schemeName);
    }
    
    @Override
    public ASTNode visitSchemaName(final SchemaNameContext ctx) {
        IdentifierSegment identifier = (IdentifierSegment) visit(ctx.identifier());
        return new SchemeNameSegment(identifier);
    }
    
    @Override
    public ASTNode visitIdentifier(final IdentifierContext ctx) {
        return new IdentifierSegment(ctx.getText());
    }

    @Override
    public ASTNode visitSelect(SelectContext ctx) {
        ConditionSegment conditionSegment = (ConditionSegment) visit(ctx.condition());
        return new SelectSegment(conditionSegment);
    }

    @Override
    public ASTNode visitCondition(ConditionContext ctx) {
        ColValueSegment colValueSegment = (ColValueSegment) visit(ctx.colValue());
        return new ConditionSegment(colValueSegment);
    }

    @Override
    public ASTNode visitColValue(ColValueContext ctx) {
        IdentifierSegment identifier = (IdentifierSegment) visit(ctx.identifier());
        return new ColValueSegment(identifier);
    }
}
