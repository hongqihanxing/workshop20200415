package shardingsphere.workshop.parser.statement.segment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shardingsphere.workshop.parser.statement.ASTNode;


@RequiredArgsConstructor
@Getter
public class ColValueSegment implements ASTNode {

  private final IdentifierSegment identifier;
}
