# Filtering Proxy

Sample application of a configurable filtering Proxy based on work by [Julien Viet](https://github.com/vietj/vertx-http-proxy).

## How it works

The whole setup is configured in `proxy.json` that needs to be available in the root of the application.
The basic settings are:

- port: on which port will the application listen. This value is overwritten by the `PORT` environment variable that is used by Heroku and other cloud providers
- targetHost: Which host to proxy. Can be DNS entry or IP name
- targetPort: Typically 80 or 443, can be different if you have a local install
- useSSL: (boolean) Shall ssl be used between Proxy and Target
- useProxy: (boolean) in some environments you have a proxy between your proxy and the target host. True to use one (useful mostly while developing)
- proxyHost: DNS or IP of the external Proxy
- proxyPort: Port of the external Proxy
- filters: Array of Json objects with filter definitions

Requests from the client are passed without modification to the target host.
The response gets matched against any of the defined filters by mime type and path (optional Regex). 
The filter then converts the byte stream into a mime type specific format and hands it to **each** subfilter for processing.

A subfilter should do one filtering task only. Subfilters can be applied multiple times using different configuration settings.
E.g. the DropTag filter can be used to clear different HTML tags by configuring it multiple times with different tag names.


## Filter definition

A filter definition has a shared set of properties:

- mimeType: mime type in lower case that filter is designed for
- path: Path after host/port that filter serves. Special case * = all
- regex: (boolean) Shall the path be evaluated as regular expresion (Java flavour)?
- class: Full qualified class name of the filter for this. This class might delegate actual filter work to subfilters
- subfilters: JSON array (can be empty) of subfilter definitions.

## Subfilter definition

A subfilter performs the actual filtering work. Currently there are subfilters for HTML (using a JSoup HTML document), JSON (using a JsonObject) and Text (using String).
To work the following parameters are needed:

- class: Fully qualified class name - must exist
- parameters: JSON object with class specific parameters (can be empty)

## Available Filters and subfilters

### net.wissel.vertx.proxy.filters.HtmlFilter
Takes in a HTML page. When the original page is sent HTTP junked, it combines the junks and renders a JSoup document.
The JSoup document is passed to the subfilters for processing. Current subfilters:

- net.wissel.vertx.proxy.filters.DropElements: requires parameter `name` to specify what tag to drop. If name is missing `body` gets dropped
- net.wissel.vertx.proxy.filters.DropLinks: required boolean parameter fullremove: if true, works like DropElements, if false removes only the `href` attribute
 
### net.wissel.vertx.proxy.filters.JsonFilter

Processes JSON resources by converting them to a JsonObject (careful when server serves large amount of Json). Current subfilters:

- net.wissel.vertx.proxy.filters.DropElements: requires parameter `path` to specify the Json target. Removes the entry
- net.wissel.vertx.proxy.filters.MaskElements:  requires parameter `path` to specify the Json target. Masks the entry

### net.wissel.vertx.proxy.filters.TextFilter

Processes a plaintext file. Hands a String to the subfilters Current subfilters:

- net.wissel.vertx.proxy.filters.text.UpperCase : Translates to upper case (not very exiting)

## Create your own Subfilters

There are Interfaces available that have one method only. To implement your own subfilter implement one of these interfaces.
Each subfilter needs a constructor that takes a `JsonObject` as parameter for its configuration (even if it doesn't used them).
Available interfaces:

- net.wissel.vertx.proxy.filters.HtmlSubFilter: implement `void apply(Document)` (from Jsoup)
- net.wissel.vertx.proxy.filters.JsonSubFilter: implement `void apply(JsonObject)`
- net.wissel.vertx.proxy.filters.TextSubFilter: implement `String apply(String)`

## Ideas

This is a capability demo. So extensions make sense. Some ideas:

- Build better caching
- Serve content through http/2 to reduce the sum of latency
- {your ideas here} 