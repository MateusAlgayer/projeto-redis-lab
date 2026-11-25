package org.acme.demo;

public sealed interface EnumInterface {
  public String getDescription();
}

enum EnumExtension implements EnumInterface {
  VALOR3;

  @Override
  public String getDescription() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getDescription'");
  }
}

enum EnumImplementation2 implements EnumInterface {
  VALOR1,
  VALOR2,
  VALOR4;

  @Override
  public String getDescription() {
    return null;
  }
}

enum EnumImplementation3 implements EnumInterface {
  ;

  @Override
  public String getDescription() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getDescription'");
  }
}

class EnumTester {
  public static String getDescriptionImpl(final EnumInterface t) {
    return t.getDescription();
  }

  public static String decideOn(final EnumInterface t) {
    return switch (t) {
      case final EnumExtension _ -> "Teste 1";
      case final EnumImplementation2 _ -> "teste 2";
    };
  }
}
