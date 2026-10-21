package com.sgopireddy.ies.retrieval;

/**
 * Corpora are kept separate rather than mixed in one index. A policy question
 * should not retrieve a hearing decision and present it as policy — they carry
 * different authority.
 */
public enum Corpus { POLICY_MANUAL, FAIR_HEARING_DECISIONS }
