package de.itm.uniluebeck.tr.wiseml.merger;

/**
 * Usage:
 * This package allows stream-based merging of WiseML data.
 * The user may supply any number of XMLStreamReader instances along
 * with a configuration object to the WiseMLMergerFactory class to
 * obtain another XMLStreamReader instance which merges the supplied
 * streams without buffering entire files or XML structures.
 * To read a XMLStreamReader and write its output directly to a
 * XMLStreamWriter, you may use the XMLPipe class.
 * 
 * Configuration:
 * To make certain adjustments to the merging process, use the
 * MergerConfiguration class from the config package along with
 * classes from the enums and structures packages.
 * The configuration can be written to and read from a Properties
 * object.
 * 
 * Implementation:
 * See the internals package for the implementation of the merger.
 */
