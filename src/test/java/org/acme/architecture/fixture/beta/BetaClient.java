package org.acme.architecture.fixture.beta;

import org.acme.architecture.fixture.alpha.AlphaClient;

public class BetaClient {

  private final AlphaClient alpha;

  public BetaClient(AlphaClient alpha) {
    this.alpha = alpha;
  }

  public String beta() {
    return "beta:" + alpha.alpha();
  }
}
