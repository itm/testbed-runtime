package de.itm.uniluebeck.tr.wiseml.merger.internals;

/**
 * Implementation:
 * Merging is done by combining equal WiseML elements on equal levels.
 * To do this, a WiseML-specific hierarchical streaming interface
 * was created (WiseMLTreeReader; see tree package). It ignores certain
 * XML aspects like comments and whitespace and relies on the WiseML grammar.
 * 
 * Packages:
 * 		merge - Element-wise merging of WiseML files represented by 
 * 		WiseMLTreeReader instances.
 * 
 * 		parse - Parser classes for reading XML data into objects from the
 * 		merger.structures package.
 * 
 * 		stream - Conversions between XMLStreamReader and WiseMLTreeReader.
 * 
 * 		tree - WiseMLTreeReader interface and classes for constructing
 * 		sub-trees from objects from the merger.structures package.
 * 		These are created by the merger classes and read by the user
 * 		through stream.WiseMLTreeToXMLStream
 * 
 * Merging process:
 * The connections of streams can be visualized like this:
 * 
 * (file_1.wiseml)    ...    (file_N.wiseml)
 * |                         |
 * FileReader                FileReader
 * |                         |
 * XMLStreamReader           XMLStreamReader
 * |                         |
 * XMLStreamToWiseMLTree	 XMLStreamToWiseMLTree
 * |                         |
 * +---------+   ...   +-----+
 *           |         |
 *         WiseMLTreeMerger
 *                |
 *         WiseMLTreeToXMLStream
 *                |
 *         XMLPipe
 *                |
 *         XMLStreamWriter
 *                |
 *         FileWriter
 *                |
 *         (output.wiseml)
 * 
 * (Note that his package only concerns WiseMLTreeMerger. The example shows a
 * setup which could be used to merge several files from the disk, writing
 * to a new one. Of course other usage scenarios are possible as well)
 */
