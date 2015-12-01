<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://neuroml.org/">
    <xsl:output omit-xml-declaration="yes" />
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="ComponentType">
        <xsl:element name="ComponentType" xmlns="http://www.neuroml.org/lems/0.9.0">
            <xsl:apply-templates select="node()|@*" />
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>