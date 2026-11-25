---
regra: CART-13
status: observado
---
# Contrato da carta na resposta e no cache

Este é o único cenário de carta que registra o JSON inteiro. Campos desconhecidos da API são ignorados.

```stub-mtg
GET /cards/386616 -> 200
{"card": {"name": "Narset", "legalities": [{"format": "Commander", "legality": "Legal"}], "campo_desconhecido": "x"}}
```

```http corpo redis
GET /carta/386616
```

```resultado
200
{
  "cacheHit" : false,
  "carta" : {
    "artist" : null,
    "cmc" : null,
    "colorIdentity" : null,
    "colors" : null,
    "flavor" : null,
    "id" : null,
    "imageUrl" : null,
    "layout" : null,
    "legalities" : [
      {
        "format" : "Commander",
        "legality" : "Legal"
      }
    ],
    "manaCost" : null,
    "multiverseid" : null,
    "name" : "Narset",
    "names" : null,
    "number" : null,
    "power" : null,
    "printings" : null,
    "rarity" : null,
    "set" : null,
    "setName" : null,
    "subtypes" : null,
    "supertypes" : null,
    "text" : null,
    "toughness" : null,
    "type" : null,
    "types" : null
  },
  "fonte" : "API",
  "tempoRestanteCache" : "~60min"
}
carta:386616  ttl=~60min
  {"artist":null,"cmc":null,"colorIdentity":null,"colors":null,"flavor":null,"id":null,"imageUrl":null,"layout":null,"legalities":[{"format":"Commander","legality":"Legal"}],"manaCost":null,"multiverseid":null,"name":"Narset","names":null,"number":null,"power":null,"printings":null,"rarity":null,"set":null,"setName":null,"subtypes":null,"supertypes":null,"text":null,"toughness":null,"type":null,"types":null}
```
