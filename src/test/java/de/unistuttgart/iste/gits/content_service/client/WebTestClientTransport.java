package de.unistuttgart.iste.gits.content_service.client;


// COPY PASTED from org.springframework.graphql.test.tester.WebTestClientTransport
// because it is not public
// author: Rossen Stoyanchev

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.GraphQlRequest;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.graphql.client.GraphQlTransport;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

public class WebTestClientTransport implements GraphQlTransport {

	private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
			new ParameterizedTypeReference<Map<String, Object>>() {};


	private final WebTestClient webTestClient;


	WebTestClientTransport(WebTestClient webTestClient) {
		Assert.notNull(webTestClient, "WebTestClient is required");
		this.webTestClient = webTestClient;
	}


	@Override
	public Mono<GraphQlResponse> execute(GraphQlRequest request) {

		Map<String, Object> responseMap = this.webTestClient.post()
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.bodyValue(request.toMap())
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody(MAP_TYPE)
				.returnResult()
				.getResponseBody();

		responseMap = (responseMap != null ? responseMap : Collections.emptyMap());
		GraphQlResponse response = GraphQlTransport.createResponse(responseMap);
		return Mono.just(response);
	}

	@Override
	public Flux<GraphQlResponse> executeSubscription(GraphQlRequest request) {
		throw new UnsupportedOperationException("Subscriptions not supported over HTTP");
	}

}