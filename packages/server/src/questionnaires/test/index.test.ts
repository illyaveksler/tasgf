import { expect, test } from "vitest";
import { Questionnaires } from "../questionnaires";

test("Hello test", () => {
  const expected = "Hello, world!";

  expect(Questionnaires.hello()).toEqual(expected);
});
