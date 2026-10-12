import { describe, expect, it } from "vitest";
import { courseConfig, studentConfig } from "./resourceConfig";
import { integerValue, validateFields } from "./validation";

describe("backend-aligned validation", () => {
  it("accepts age zero and rejects zero course credits", () => {
    expect(
      validateFields(studentConfig.fields, {
        studentCode: "S1",
        name: "Student",
        age: "0",
      }),
    ).toEqual({});
    expect(
      validateFields(courseConfig.fields, {
        code: "C1",
        name: "Course",
        credits: "0",
      }),
    ).toHaveProperty("credits");
  });

  it.each(["-1", "2.5", "1e2", "", "2147483648", "NaN"])(
    "rejects invalid integer input %s",
    (value) => {
      expect(integerValue(value, 0)).toBeNull();
    },
  );

  it("rejects whitespace-only identifiers/names and enforces DTO lengths", () => {
    expect(
      validateFields(studentConfig.fields, {
        studentCode: " ",
        name: " ",
        age: "20",
      }),
    ).toEqual({
      studentCode: "Student code is required.",
      name: "Full name is required.",
    });
    expect(
      validateFields(courseConfig.fields, {
        code: "C".repeat(21),
        name: "N".repeat(101),
        credits: "4",
      }),
    ).toHaveProperty("code");
    expect(
      validateFields(courseConfig.fields, {
        code: "C1",
        name: "N".repeat(101),
        credits: "4",
      }),
    ).toHaveProperty("name");
  });

  it("trims text and sends numeric JSON values with exact backend field names", () => {
    expect(
      studentConfig.input({
        studentCode: " S1 ",
        name: " Student ",
        age: "20",
      }),
    ).toEqual({ studentCode: "S1", name: "Student", age: 20 });
    expect(
      courseConfig.input({ code: " C1 ", name: " Course ", credits: "4" }),
    ).toEqual({ code: "C1", name: "Course", credits: 4 });
  });
});
