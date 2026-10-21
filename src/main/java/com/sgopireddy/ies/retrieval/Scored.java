package com.sgopireddy.ies.retrieval;

/** A value with a relevance score. */
public record Scored<T>(T value, double score) {}
