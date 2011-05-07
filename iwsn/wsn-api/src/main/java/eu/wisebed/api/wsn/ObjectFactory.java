
package eu.wisebed.api.wsn;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the eu.wisebed.api.wsn package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _GetFiltersResponse_QNAME = new QName("urn:WSNService", "getFiltersResponse");
    private final static QName _FlashProgramsResponse_QNAME = new QName("urn:WSNService", "flashProgramsResponse");
    private final static QName _DisableNode_QNAME = new QName("urn:WSNService", "disableNode");
    private final static QName _DisableNodeResponse_QNAME = new QName("urn:WSNService", "disableNodeResponse");
    private final static QName _DestroyVirtualLinkResponse_QNAME = new QName("urn:WSNService", "destroyVirtualLinkResponse");
    private final static QName _SendResponse_QNAME = new QName("urn:WSNService", "sendResponse");
    private final static QName _EnablePhysicalLink_QNAME = new QName("urn:WSNService", "enablePhysicalLink");
    private final static QName _DisablePhysicalLinkResponse_QNAME = new QName("urn:WSNService", "disablePhysicalLinkResponse");
    private final static QName _AddController_QNAME = new QName("urn:WSNService", "addController");
    private final static QName _EnableNodeResponse_QNAME = new QName("urn:WSNService", "enableNodeResponse");
    private final static QName _GetSupportedChannelHandlers_QNAME = new QName("urn:WSNService", "getSupportedChannelHandlers");
    private final static QName _FlashPrograms_QNAME = new QName("urn:WSNService", "flashPrograms");
    private final static QName _AreNodesAlive_QNAME = new QName("urn:WSNService", "areNodesAlive");
    private final static QName _ResetNodesResponse_QNAME = new QName("urn:WSNService", "resetNodesResponse");
    private final static QName _RemoveController_QNAME = new QName("urn:WSNService", "removeController");
    private final static QName _SetVirtualLink_QNAME = new QName("urn:WSNService", "setVirtualLink");
    private final static QName _RemoveControllerResponse_QNAME = new QName("urn:WSNService", "removeControllerResponse");
    private final static QName _GetFilters_QNAME = new QName("urn:WSNService", "getFilters");
    private final static QName _AddControllerResponse_QNAME = new QName("urn:WSNService", "addControllerResponse");
    private final static QName _EnableNode_QNAME = new QName("urn:WSNService", "enableNode");
    private final static QName _DisablePhysicalLink_QNAME = new QName("urn:WSNService", "disablePhysicalLink");
    private final static QName _GetSupportedChannelHandlersResponse_QNAME = new QName("urn:WSNService", "getSupportedChannelHandlersResponse");
    private final static QName _SetChannelPipelineResponse_QNAME = new QName("urn:WSNService", "setChannelPipelineResponse");
    private final static QName _Send_QNAME = new QName("urn:WSNService", "send");
    private final static QName _EnablePhysicalLinkResponse_QNAME = new QName("urn:WSNService", "enablePhysicalLinkResponse");
    private final static QName _SetVirtualLinkResponse_QNAME = new QName("urn:WSNService", "setVirtualLinkResponse");
    private final static QName _DestroyVirtualLink_QNAME = new QName("urn:WSNService", "destroyVirtualLink");
    private final static QName _GetVersionResponse_QNAME = new QName("urn:WSNService", "getVersionResponse");
    private final static QName _GetVersion_QNAME = new QName("urn:WSNService", "getVersion");
    private final static QName _ResetNodes_QNAME = new QName("urn:WSNService", "resetNodes");
    private final static QName _AreNodesAliveResponse_QNAME = new QName("urn:WSNService", "areNodesAliveResponse");
    private final static QName _SetChannelPipeline_QNAME = new QName("urn:WSNService", "setChannelPipeline");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: eu.wisebed.api.wsn
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link RemoveController }
     * 
     */
    public RemoveController createRemoveController() {
        return new RemoveController();
    }

    /**
     * Create an instance of {@link SetChannelPipeline }
     * 
     */
    public SetChannelPipeline createSetChannelPipeline() {
        return new SetChannelPipeline();
    }

    /**
     * Create an instance of {@link RemoveControllerResponse }
     * 
     */
    public RemoveControllerResponse createRemoveControllerResponse() {
        return new RemoveControllerResponse();
    }

    /**
     * Create an instance of {@link GetSupportedChannelHandlersResponse }
     * 
     */
    public GetSupportedChannelHandlersResponse createGetSupportedChannelHandlersResponse() {
        return new GetSupportedChannelHandlersResponse();
    }

    /**
     * Create an instance of {@link ChannelHandlerConfiguration }
     * 
     */
    public ChannelHandlerConfiguration createChannelHandlerConfiguration() {
        return new ChannelHandlerConfiguration();
    }

    /**
     * Create an instance of {@link SetChannelPipelineResponse }
     * 
     */
    public SetChannelPipelineResponse createSetChannelPipelineResponse() {
        return new SetChannelPipelineResponse();
    }

    /**
     * Create an instance of {@link EnableNode }
     * 
     */
    public EnableNode createEnableNode() {
        return new EnableNode();
    }

    /**
     * Create an instance of {@link AreNodesAliveResponse }
     * 
     */
    public AreNodesAliveResponse createAreNodesAliveResponse() {
        return new AreNodesAliveResponse();
    }

    /**
     * Create an instance of {@link ResetNodes }
     * 
     */
    public ResetNodes createResetNodes() {
        return new ResetNodes();
    }

    /**
     * Create an instance of {@link EnableNodeResponse }
     * 
     */
    public EnableNodeResponse createEnableNodeResponse() {
        return new EnableNodeResponse();
    }

    /**
     * Create an instance of {@link AddControllerResponse }
     * 
     */
    public AddControllerResponse createAddControllerResponse() {
        return new AddControllerResponse();
    }

    /**
     * Create an instance of {@link FlashPrograms }
     * 
     */
    public FlashPrograms createFlashPrograms() {
        return new FlashPrograms();
    }

    /**
     * Create an instance of {@link Send }
     * 
     */
    public Send createSend() {
        return new Send();
    }

    /**
     * Create an instance of {@link FlashProgramsResponse }
     * 
     */
    public FlashProgramsResponse createFlashProgramsResponse() {
        return new FlashProgramsResponse();
    }

    /**
     * Create an instance of {@link DestroyVirtualLinkResponse }
     * 
     */
    public DestroyVirtualLinkResponse createDestroyVirtualLinkResponse() {
        return new DestroyVirtualLinkResponse();
    }

    /**
     * Create an instance of {@link DisableNode }
     * 
     */
    public DisableNode createDisableNode() {
        return new DisableNode();
    }

    /**
     * Create an instance of {@link GetSupportedChannelHandlers }
     * 
     */
    public GetSupportedChannelHandlers createGetSupportedChannelHandlers() {
        return new GetSupportedChannelHandlers();
    }

    /**
     * Create an instance of {@link DisableNodeResponse }
     * 
     */
    public DisableNodeResponse createDisableNodeResponse() {
        return new DisableNodeResponse();
    }

    /**
     * Create an instance of {@link EnablePhysicalLink }
     * 
     */
    public EnablePhysicalLink createEnablePhysicalLink() {
        return new EnablePhysicalLink();
    }

    /**
     * Create an instance of {@link DestroyVirtualLink }
     * 
     */
    public DestroyVirtualLink createDestroyVirtualLink() {
        return new DestroyVirtualLink();
    }

    /**
     * Create an instance of {@link AddController }
     * 
     */
    public AddController createAddController() {
        return new AddController();
    }

    /**
     * Create an instance of {@link GetFiltersResponse }
     * 
     */
    public GetFiltersResponse createGetFiltersResponse() {
        return new GetFiltersResponse();
    }

    /**
     * Create an instance of {@link ProgramMetaData }
     * 
     */
    public ProgramMetaData createProgramMetaData() {
        return new ProgramMetaData();
    }

    /**
     * Create an instance of {@link SetVirtualLinkResponse }
     * 
     */
    public SetVirtualLinkResponse createSetVirtualLinkResponse() {
        return new SetVirtualLinkResponse();
    }

    /**
     * Create an instance of {@link SendResponse }
     * 
     */
    public SendResponse createSendResponse() {
        return new SendResponse();
    }

    /**
     * Create an instance of {@link ChannelHandlerDescription }
     * 
     */
    public ChannelHandlerDescription createChannelHandlerDescription() {
        return new ChannelHandlerDescription();
    }

    /**
     * Create an instance of {@link GetVersionResponse }
     * 
     */
    public GetVersionResponse createGetVersionResponse() {
        return new GetVersionResponse();
    }

    /**
     * Create an instance of {@link Program }
     * 
     */
    public Program createProgram() {
        return new Program();
    }

    /**
     * Create an instance of {@link EnablePhysicalLinkResponse }
     * 
     */
    public EnablePhysicalLinkResponse createEnablePhysicalLinkResponse() {
        return new EnablePhysicalLinkResponse();
    }

    /**
     * Create an instance of {@link ResetNodesResponse }
     * 
     */
    public ResetNodesResponse createResetNodesResponse() {
        return new ResetNodesResponse();
    }

    /**
     * Create an instance of {@link AreNodesAlive }
     * 
     */
    public AreNodesAlive createAreNodesAlive() {
        return new AreNodesAlive();
    }

    /**
     * Create an instance of {@link GetVersion }
     * 
     */
    public GetVersion createGetVersion() {
        return new GetVersion();
    }

    /**
     * Create an instance of {@link SetVirtualLink }
     * 
     */
    public SetVirtualLink createSetVirtualLink() {
        return new SetVirtualLink();
    }

    /**
     * Create an instance of {@link DisablePhysicalLinkResponse }
     * 
     */
    public DisablePhysicalLinkResponse createDisablePhysicalLinkResponse() {
        return new DisablePhysicalLinkResponse();
    }

    /**
     * Create an instance of {@link GetFilters }
     * 
     */
    public GetFilters createGetFilters() {
        return new GetFilters();
    }

    /**
     * Create an instance of {@link DisablePhysicalLink }
     * 
     */
    public DisablePhysicalLink createDisablePhysicalLink() {
        return new DisablePhysicalLink();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFiltersResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "getFiltersResponse")
    public JAXBElement<GetFiltersResponse> createGetFiltersResponse(GetFiltersResponse value) {
        return new JAXBElement<GetFiltersResponse>(_GetFiltersResponse_QNAME, GetFiltersResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FlashProgramsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "flashProgramsResponse")
    public JAXBElement<FlashProgramsResponse> createFlashProgramsResponse(FlashProgramsResponse value) {
        return new JAXBElement<FlashProgramsResponse>(_FlashProgramsResponse_QNAME, FlashProgramsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DisableNode }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "disableNode")
    public JAXBElement<DisableNode> createDisableNode(DisableNode value) {
        return new JAXBElement<DisableNode>(_DisableNode_QNAME, DisableNode.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DisableNodeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "disableNodeResponse")
    public JAXBElement<DisableNodeResponse> createDisableNodeResponse(DisableNodeResponse value) {
        return new JAXBElement<DisableNodeResponse>(_DisableNodeResponse_QNAME, DisableNodeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SetVirtualLinkResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "destroyVirtualLinkResponse")
    public JAXBElement<SetVirtualLinkResponse> createDestroyVirtualLinkResponse(SetVirtualLinkResponse value) {
        return new JAXBElement<SetVirtualLinkResponse>(_DestroyVirtualLinkResponse_QNAME, SetVirtualLinkResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SendResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "sendResponse")
    public JAXBElement<SendResponse> createSendResponse(SendResponse value) {
        return new JAXBElement<SendResponse>(_SendResponse_QNAME, SendResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EnablePhysicalLink }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "enablePhysicalLink")
    public JAXBElement<EnablePhysicalLink> createEnablePhysicalLink(EnablePhysicalLink value) {
        return new JAXBElement<EnablePhysicalLink>(_EnablePhysicalLink_QNAME, EnablePhysicalLink.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DisablePhysicalLinkResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "disablePhysicalLinkResponse")
    public JAXBElement<DisablePhysicalLinkResponse> createDisablePhysicalLinkResponse(DisablePhysicalLinkResponse value) {
        return new JAXBElement<DisablePhysicalLinkResponse>(_DisablePhysicalLinkResponse_QNAME, DisablePhysicalLinkResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddController }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "addController")
    public JAXBElement<AddController> createAddController(AddController value) {
        return new JAXBElement<AddController>(_AddController_QNAME, AddController.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EnableNodeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "enableNodeResponse")
    public JAXBElement<EnableNodeResponse> createEnableNodeResponse(EnableNodeResponse value) {
        return new JAXBElement<EnableNodeResponse>(_EnableNodeResponse_QNAME, EnableNodeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSupportedChannelHandlers }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "getSupportedChannelHandlers")
    public JAXBElement<GetSupportedChannelHandlers> createGetSupportedChannelHandlers(GetSupportedChannelHandlers value) {
        return new JAXBElement<GetSupportedChannelHandlers>(_GetSupportedChannelHandlers_QNAME, GetSupportedChannelHandlers.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FlashPrograms }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "flashPrograms")
    public JAXBElement<FlashPrograms> createFlashPrograms(FlashPrograms value) {
        return new JAXBElement<FlashPrograms>(_FlashPrograms_QNAME, FlashPrograms.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AreNodesAlive }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "areNodesAlive")
    public JAXBElement<AreNodesAlive> createAreNodesAlive(AreNodesAlive value) {
        return new JAXBElement<AreNodesAlive>(_AreNodesAlive_QNAME, AreNodesAlive.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ResetNodesResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "resetNodesResponse")
    public JAXBElement<ResetNodesResponse> createResetNodesResponse(ResetNodesResponse value) {
        return new JAXBElement<ResetNodesResponse>(_ResetNodesResponse_QNAME, ResetNodesResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveController }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "removeController")
    public JAXBElement<RemoveController> createRemoveController(RemoveController value) {
        return new JAXBElement<RemoveController>(_RemoveController_QNAME, RemoveController.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SetVirtualLink }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "setVirtualLink")
    public JAXBElement<SetVirtualLink> createSetVirtualLink(SetVirtualLink value) {
        return new JAXBElement<SetVirtualLink>(_SetVirtualLink_QNAME, SetVirtualLink.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveControllerResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "removeControllerResponse")
    public JAXBElement<RemoveControllerResponse> createRemoveControllerResponse(RemoveControllerResponse value) {
        return new JAXBElement<RemoveControllerResponse>(_RemoveControllerResponse_QNAME, RemoveControllerResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFilters }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "getFilters")
    public JAXBElement<GetFilters> createGetFilters(GetFilters value) {
        return new JAXBElement<GetFilters>(_GetFilters_QNAME, GetFilters.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddControllerResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "addControllerResponse")
    public JAXBElement<AddControllerResponse> createAddControllerResponse(AddControllerResponse value) {
        return new JAXBElement<AddControllerResponse>(_AddControllerResponse_QNAME, AddControllerResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EnableNode }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "enableNode")
    public JAXBElement<EnableNode> createEnableNode(EnableNode value) {
        return new JAXBElement<EnableNode>(_EnableNode_QNAME, EnableNode.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DisablePhysicalLink }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "disablePhysicalLink")
    public JAXBElement<DisablePhysicalLink> createDisablePhysicalLink(DisablePhysicalLink value) {
        return new JAXBElement<DisablePhysicalLink>(_DisablePhysicalLink_QNAME, DisablePhysicalLink.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSupportedChannelHandlersResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "getSupportedChannelHandlersResponse")
    public JAXBElement<GetSupportedChannelHandlersResponse> createGetSupportedChannelHandlersResponse(GetSupportedChannelHandlersResponse value) {
        return new JAXBElement<GetSupportedChannelHandlersResponse>(_GetSupportedChannelHandlersResponse_QNAME, GetSupportedChannelHandlersResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SetChannelPipelineResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "setChannelPipelineResponse")
    public JAXBElement<SetChannelPipelineResponse> createSetChannelPipelineResponse(SetChannelPipelineResponse value) {
        return new JAXBElement<SetChannelPipelineResponse>(_SetChannelPipelineResponse_QNAME, SetChannelPipelineResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Send }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "send")
    public JAXBElement<Send> createSend(Send value) {
        return new JAXBElement<Send>(_Send_QNAME, Send.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EnablePhysicalLinkResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "enablePhysicalLinkResponse")
    public JAXBElement<EnablePhysicalLinkResponse> createEnablePhysicalLinkResponse(EnablePhysicalLinkResponse value) {
        return new JAXBElement<EnablePhysicalLinkResponse>(_EnablePhysicalLinkResponse_QNAME, EnablePhysicalLinkResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SetVirtualLinkResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "setVirtualLinkResponse")
    public JAXBElement<SetVirtualLinkResponse> createSetVirtualLinkResponse(SetVirtualLinkResponse value) {
        return new JAXBElement<SetVirtualLinkResponse>(_SetVirtualLinkResponse_QNAME, SetVirtualLinkResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DestroyVirtualLink }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "destroyVirtualLink")
    public JAXBElement<DestroyVirtualLink> createDestroyVirtualLink(DestroyVirtualLink value) {
        return new JAXBElement<DestroyVirtualLink>(_DestroyVirtualLink_QNAME, DestroyVirtualLink.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetVersionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "getVersionResponse")
    public JAXBElement<GetVersionResponse> createGetVersionResponse(GetVersionResponse value) {
        return new JAXBElement<GetVersionResponse>(_GetVersionResponse_QNAME, GetVersionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetVersion }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "getVersion")
    public JAXBElement<GetVersion> createGetVersion(GetVersion value) {
        return new JAXBElement<GetVersion>(_GetVersion_QNAME, GetVersion.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ResetNodes }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "resetNodes")
    public JAXBElement<ResetNodes> createResetNodes(ResetNodes value) {
        return new JAXBElement<ResetNodes>(_ResetNodes_QNAME, ResetNodes.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AreNodesAliveResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "areNodesAliveResponse")
    public JAXBElement<AreNodesAliveResponse> createAreNodesAliveResponse(AreNodesAliveResponse value) {
        return new JAXBElement<AreNodesAliveResponse>(_AreNodesAliveResponse_QNAME, AreNodesAliveResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SetChannelPipeline }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:WSNService", name = "setChannelPipeline")
    public JAXBElement<SetChannelPipeline> createSetChannelPipeline(SetChannelPipeline value) {
        return new JAXBElement<SetChannelPipeline>(_SetChannelPipeline_QNAME, SetChannelPipeline.class, null, value);
    }

}
