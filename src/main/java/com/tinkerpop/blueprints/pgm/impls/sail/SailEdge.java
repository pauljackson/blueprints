package com.tinkerpop.blueprints.pgm.impls.sail;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Vertex;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class SailEdge implements Edge {

    protected Statement statement;
    protected SailConnection sailConnection;

    private static final String NAMED_GRAPH_PROPERTY = "RDF graph edges can only have named graph (ng) properties";

    protected final static String NAMED_GRAPH = "ng";

    public SailEdge(final Statement statement, final SailConnection sailConnection) {
        this.statement = statement;
        this.sailConnection = sailConnection;
    }

    public Statement getRawEdge() {
        return this.statement;
    }

    public String getLabel() {
        return this.statement.getPredicate().stringValue();
    }

    public Set<String> getPropertyKeys() {
        Set<String> keys = new HashSet<String>();
        if (null != this.statement.getContext())
            keys.add(NAMED_GRAPH);
        return keys;
    }

    public Object getProperty(final String key) {
        if (key.equals(NAMED_GRAPH))
            return this.statement.getContext().stringValue();
        else
            return null;
    }

    public void setProperty(final String key, final Object value) {
        if (key.equals(NAMED_GRAPH)) {
            try {
                URI namedGraph = new URIImpl(SailGraph.prefixToNamespace(value.toString(), this.sailConnection));
                SailHelper.removeStatement(this.statement, this.sailConnection);
                this.statement = new ContextStatementImpl(this.statement.getSubject(), this.statement.getPredicate(), this.statement.getObject(), namedGraph);
                SailHelper.addStatement(this.statement, this.sailConnection);
                this.sailConnection.commit();
            } catch (SailException e) {
                throw new RuntimeException(e.getMessage());
            }
        } else {
            throw new RuntimeException(NAMED_GRAPH_PROPERTY);
        }
    }

    public Object removeProperty(final String key) {
        if (key.equals(NAMED_GRAPH)) {
            try {
                Resource ng = this.statement.getContext();
                SailHelper.removeStatement(this.statement, this.sailConnection);
                this.statement = new StatementImpl(this.statement.getSubject(), this.statement.getPredicate(), this.statement.getObject());
                SailHelper.addStatement(this.statement, this.sailConnection);
                this.sailConnection.commit();
                return ng;
            } catch (SailException e) {
                throw new RuntimeException(e.getMessage());
            }
        } else {
            throw new RuntimeException(NAMED_GRAPH_PROPERTY);
        }
    }

    public Vertex getInVertex() {
        return new SailVertex(this.statement.getObject(), this.sailConnection);
    }

    public Vertex getOutVertex() {
        return new SailVertex(this.statement.getSubject(), this.sailConnection);
    }

    public Statement getRawStatement() {
        return this.statement;
    }

    public String toString() {
        final String outVertex = SailGraph.namespaceToPrefix(this.statement.getSubject().stringValue(), this.sailConnection);
        final String edgeLabel = SailGraph.namespaceToPrefix(this.statement.getPredicate().stringValue(), this.sailConnection);
        String inVertex;
        if (this.statement.getObject() instanceof Resource)
            inVertex = SailGraph.namespaceToPrefix(this.statement.getObject().stringValue(), this.sailConnection);
        else
            inVertex = literalString((Literal) this.statement.getObject());

        String namedGraph = null;
        if (null != this.statement.getContext()) {
            namedGraph = SailGraph.namespaceToPrefix(this.statement.getContext().stringValue(), this.sailConnection);
        }

        String edgeString = "e[" + outVertex + " - " + edgeLabel + " -> " + inVertex + "]";
        if (null != namedGraph) {
            edgeString = edgeString + "<" + namedGraph + ">";
        }

        return edgeString;
    }

    private String literalString(final Literal literal) {
        final String language = literal.getLanguage();
        final URI datatype = literal.getDatatype();
        if (null != datatype) {
            return "\"" + literal.getLabel() + "\"^^<" + SailGraph.namespaceToPrefix(datatype.stringValue(), this.sailConnection) + ">";
        } else if (null != language) {
            return "\"" + literal.getLabel() + "\"@" + language;
        } else {
            return "\"" + literal.getLabel() + "\"";
        }
    }

    public boolean equals(final Object object) {
        return object instanceof SailEdge && ((SailEdge) object).getId().equals(this.getId());
    }

    public int hashCode() {
        return this.getId().hashCode();
    }

    public Object getId() {
        //return this.statement.hashCode();
        if (null != this.statement.getContext())
            return "(" + this.statement.getSubject() + ", " + this.statement.getPredicate() + ", " + this.statement.getObject() + ") [" + this.statement.getContext() + "]";
        else
            return "(" + this.statement.getSubject() + ", " + this.statement.getPredicate() + ", " + this.statement.getObject() + ")";
    }
}
