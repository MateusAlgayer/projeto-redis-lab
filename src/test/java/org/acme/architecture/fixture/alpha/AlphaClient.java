package org.acme.architecture.fixture.alpha;

import org.acme.architecture.fixture.beta.BetaClient;

public class AlphaClient {

  private final BetaClient beta;

  public AlphaClient(BetaClient beta) {
    this.beta = beta;
  }

  public String alpha() {
    return "alpha:" + beta.beta();
  }
}
