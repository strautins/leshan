package org.eclipse.leshan.server.demo.mt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileResource extends CoapResource {

    private static final Logger LOG = LoggerFactory.getLogger(FileResource.class.getName());
    
    private final NetworkConfig config;
    /**
     * Files root directory.
     */
    private final File filesRoot;

    /**
     * Create CoAP file resource.
     * 
     * @param config network configuration
     * @param coapRootPath CoAP resource (base) name
     * @param filesRoot path to file root
     */
    public FileResource(NetworkConfig config, String coapRootPath, File filesRoot) {
        super(coapRootPath);
        this.config = config;
        this.filesRoot = filesRoot;
	}

    /*
     * Override the default behavior so that requests to sub resources
     * (typically /{path}/{file-name}) are handled by /file resource.
     */
    @Override
    public Resource getChild(String name) {
        return this;
    }

    @Override
    public void handleRequest(Exchange exchange) {
        try {
            super.handleRequest(exchange);
        } catch (Exception e) {
            LOG.error("Exception while handling a request on the {} resource", getName(), e);
            exchange.sendResponse(new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @Override
    public void handleGET(final CoapExchange exchange) {
        Request request = exchange.advanced().getRequest();
        LOG.info("Get received : {}", request);

        int accept = request.getOptions().getAccept();
        if (MediaTypeRegistry.UNDEFINED == accept) {
            accept = MediaTypeRegistry.APPLICATION_OCTET_STREAM;
        } else if (MediaTypeRegistry.APPLICATION_OCTET_STREAM != accept) {
            exchange.respond(CoAP.ResponseCode.UNSUPPORTED_CONTENT_FORMAT);
            return;
        }

        String myURI = getURI() + "/";
        String path = "/" + request.getOptions().getUriPathString();
        if (!path.startsWith(myURI)) {
            LOG.info("Request {} does not match {}!", path, myURI);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
            return;
        }
        path = path.substring(myURI.length());
        if (request.getOptions().hasBlock2()) {
            LOG.info("Send file {} {}", path, request.getOptions().getBlock2());
        } else {
            LOG.info("Send file {}", path);
        }
        File file = new File(filesRoot, path);
        if (!file.exists() || !file.isFile()) {
            LOG.warn("File {} doesn't exist!", file.getAbsolutePath());
            exchange.respond(CoAP.ResponseCode.NOT_FOUND);
            return;
        }
        if (!checkFileLocation(file, filesRoot)) {
            LOG.warn("File {} is not in {}!", file.getAbsolutePath(), filesRoot.getAbsolutePath());
            exchange.respond(CoAP.ResponseCode.UNAUTHORIZED);
            return;
        }

        if (!file.canRead()) {
            LOG.warn("File {} is not readable!", file.getAbsolutePath());
            exchange.respond(CoAP.ResponseCode.UNAUTHORIZED);
            return;
        }
        long maxLength = config.getInt(NetworkConfig.Keys.MAX_RESOURCE_BODY_SIZE);
        long length = file.length();
        if (length > maxLength) {
            LOG.warn("File {} is too large {} (max.: {})!", file.getAbsolutePath(), length, maxLength);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
            return;
        }
        try (InputStream in = new FileInputStream(file)) {
            byte[] content = new byte[(int) length];
            int r = in.read(content);
            if (length == r) {
                Response response = new Response(CoAP.ResponseCode.CONTENT);
                response.setPayload(content);
                response.getOptions().setSize2((int) length);
                response.getOptions().setContentFormat(accept);
                exchange.respond(response);
            } else {
                LOG.warn("File {} could not be read in!", file.getAbsolutePath());
                exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
            }
        } catch (IOException ex) {
            LOG.warn("File {}:", file.getAbsolutePath(), ex);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Check, if file is located in root.
     * 
     * Detect attacks via "../.../../file".
     * 
     * @param file file to check
     * @param root file root
     * @return true, if file is locate in root (or a sub-folder of root),
     *         false, otherwise.
     */
    private boolean checkFileLocation(File file, File root) {
        try {
            return file.getCanonicalPath().startsWith(root.getCanonicalPath());
        } catch (IOException ex) {
            LOG.warn("File {0}:", file.getAbsolutePath(), ex);
            return false;
        }
    }
}