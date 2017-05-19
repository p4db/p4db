package org.netarch.odb.runtime;

public class MatchResult {
    private Short metadataMatchResult;
    private Short stdMetadataMatchResult;
    private Short headerMatchResult;
    private Long matchResult;

    /**
     * Create a match result from the user metadata  standard metadata and header.
     *
     * @param meta   user metadata
     * @param std    standard metadata
     * @param header packet header
     */
    public MatchResult(Short std, Short meta, Short header) {
        this.metadataMatchResult = meta;
        this.stdMetadataMatchResult = std;
        this.headerMatchResult = header;

        this.matchResult = Long.valueOf(0);
        this.matchResult &= (short) headerMatchResult;
        this.matchResult <<= 16;
        this.matchResult &= (short) metadataMatchResult;
        this.matchResult <<= 16;
        this.matchResult &= (short) stdMetadataMatchResult;
    }

    /**
     * Create a match result from the matchResult.
     *
     * @param matchResult match result
     */
    public MatchResult(Long matchResult) {
        this.matchResult = matchResult;
        this.stdMetadataMatchResult = (short) (this.matchResult & 0xFFFF);
        this.metadataMatchResult = (short) ((this.matchResult >> 16) & 0xFFFF);
        this.headerMatchResult = (short) (((this.matchResult >> 32)) & 0xFFFF);
    }

    /**
     * Get the header.
     *
     * @return header
     */
    public Short getHeader() {
        return headerMatchResult;
    }

    /**
     * Get the metadata.
     *
     * @return metadata
     */
    public Short getMetadata() {
        return metadataMatchResult;
    }

    /**
     * Get the std metadata.
     *
     * @return standard metadata.
     */
    public Short getStdMetadata() {
        return stdMetadataMatchResult;
    }

    /**
     * Get the match result.
     *
     * @return standard metadata match result
     */
    public Long getStdMetadataMatchResult() {
        return matchResult & 0xFFFFL;
    }

    /**
     * Get metadata matc hresult.
     *
     * @return match result
     */
    public Long getMetadataMatchResult() {
        long mask = 0xFFFFL << 16;
        return matchResult & mask;
    }

    /**
     * Get heaer match result
     *
     * @return header match result
     */
    public Long getHeaderMatchResult() {
        long mask = 0xFFFFL << 32;
        return matchResult & mask;
    }

    /**
     * Get match result.
     *
     * @return match result
     */
    public Long getMatchResult() {
        return matchResult;
    }

    public MatchResult createResult(Short std, Short meta, Short header) {
        return new MatchResult(std, meta, header);
    }
}
